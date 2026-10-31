#!/usr/bin/env python3
"""Build the fixed Metal Gear MD ROM from the original v1.2 .gen.

Patches (details in README.md):
  1. Full work-RAM clear at boot (was 0xFF0000-0xFF7C53 only).
  2. SAVE syncs live items/ammo/rank into the snapshot   (patches/sync_inventory.s).
  3. Z80 handshake timeouts: 12 BUSACK waits + 2 ready-flag waits no longer
     hang forever                                         (patches/z80_timeouts.s).
  4. Header ROM end + checksum.

Safety:
  * every patch checks the original bytes first, so it only applies to v1.2;
  * after building, the output is diffed against the source and every changed
    byte must belong to a patch (or the header checksum/ROM-end fields).

Needs only the Python standard library. Re-assembling the patch routines needs
binutils for m68k (apt: binutils-m68k-linux-gnu); the committed .bin/.sym files
are used as-is when they are up to date.

The same ROM is written twice, as .gen and as .bin (flashcart menus often list
only one of the two), with identical contents.

Usage:
  build_fixed_rom.py [SOURCE] [-o OUTPUT] [--rebuild-patches] [--force]
"""
from __future__ import annotations

import argparse
import hashlib
import shutil
import subprocess
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
PATCHES = HERE / "patches"
ORIGINAL_NAME = "Metal Gear (World) (v1.2) (Aftermarket) (Unl).gen"
FIXED_NAME = "Metal Gear (World) (v1.2) (Aftermarket) (Unl) [fixed].gen"
ORIGINAL_SHA256 = "1707f5534028cd4a3d049a552b1567fc7a3100598b450a976ed0690754be8f6a"

ROM_SIZE = 0x200000
SYNC_ADDR = 0x1BD880   # sync_inventory (position independent)
Z80_ADDR = 0x1BD900    # z80_timeouts (linked at this address by patches/build_patches.sh)
AREA_END = 0x1BD9FF    # last byte of the free area; also the new header "ROM end"
NOP = b"\x4e\x71"


class BuildError(Exception):
    pass


def jsr(addr: int) -> bytes:
    return bytes.fromhex("4eb9") + addr.to_bytes(4, "big")


def jmp(addr: int) -> bytes:
    return bytes.fromhex("4ef9") + addr.to_bytes(4, "big")


def header_checksum(rom: bytes | bytearray, end: int) -> int:
    """Genesis checksum: 16-bit sum of the words from 0x200 up to ROM end (inclusive)."""
    return sum(int.from_bytes(rom[i:i + 2], "big") for i in range(0x200, end, 2)) & 0xFFFF


# --------------------------------------------------------------------------- patch binaries

def ensure_patch_binaries(rebuild: bool) -> dict[str, int]:
    """Make sure patches/*.bin and *.sym match the .s sources; return the symbol table."""
    pairs = [(PATCHES / "sync_inventory.s", PATCHES / "sync_inventory.bin"),
             (PATCHES / "z80_timeouts.s", PATCHES / "z80_timeouts.bin"),
             (PATCHES / "z80_timeouts.s", PATCHES / "z80_timeouts.sym")]
    stale = rebuild or any(not out.exists() or out.stat().st_mtime < src.stat().st_mtime
                           for src, out in pairs)
    if stale:
        if shutil.which("m68k-linux-gnu-as") is None:
            raise BuildError("patch binaries are missing or older than their .s sources and the m68k "
                             "toolchain is not installed (apt install binutils-m68k-linux-gnu)")
        print("assembling patch routines (patches/build_patches.sh)")
        subprocess.run([str(PATCHES / "build_patches.sh")], check=True)
    sym: dict[str, int] = {}
    for line in (PATCHES / "z80_timeouts.sym").read_text().splitlines():
        name, addr = line.split()
        sym[name] = int(addr, 16)
    for needed in ("wait_busack", "flagwait_fd14", "flagwait_23980"):
        if needed not in sym:
            raise BuildError(f"symbol {needed} missing from patches/z80_timeouts.sym")
    if sym["wait_busack"] != Z80_ADDR:
        raise BuildError(f"z80_timeouts was linked at {sym['wait_busack']:#x}, build script expects {Z80_ADDR:#x}; "
                         "keep -Ttext in patches/build_patches.sh in sync with Z80_ADDR")
    return sym


# --------------------------------------------------------------------------- patching

class Patcher:
    def __init__(self, rom: bytearray):
        self.rom = rom
        self.touched: list[tuple[int, int]] = []   # (start, end exclusive) of every modified range

    def write(self, addr: int, data: bytes) -> None:
        self.rom[addr:addr + len(data)] = data
        self.touched.append((addr, addr + len(data)))

    def patch(self, addr: int, old_hex: str, new: bytes) -> None:
        """Replace the instruction bytes `old_hex` at `addr` by `new`, padding with NOPs."""
        old = bytes.fromhex(old_hex)
        found = bytes(self.rom[addr:addr + len(old)])
        if found != old:
            raise BuildError(f"unexpected bytes at {addr:#x}: found {found.hex()}, expected {old.hex()} "
                             "(is the source really v1.2?)")
        if len(new) > len(old) or (len(old) - len(new)) % 2:
            raise BuildError(f"replacement for {addr:#x} does not fit the original instructions")
        self.write(addr, new + NOP * ((len(old) - len(new)) // 2))


def apply_patches(source: bytes, sym: dict[str, int]) -> tuple[bytearray, list[tuple[int, int]]]:
    rom = bytearray(source)
    p = Patcher(rom)
    sync = (PATCHES / "sync_inventory.bin").read_bytes()
    z80 = (PATCHES / "z80_timeouts.bin").read_bytes()

    # the free area must be empty and big enough
    if SYNC_ADDR + len(sync) > Z80_ADDR or Z80_ADDR + len(z80) > AREA_END + 1:
        raise BuildError("patch routines do not fit in the free area")
    if any(rom[SYNC_ADDR:AREA_END + 1]):
        raise BuildError(f"free area {SYNC_ADDR:#x}-{AREA_END:#x} is not empty in the source ROM")
    p.write(SYNC_ADDR, sync)
    p.write(Z80_ADDR, z80)

    # 1. full work-RAM clear: movew #7956,d7 -> movew #16379,d7
    #    (clears 0xFF0000-0xFFFFEF; keeps the return address stored at 0xFFFFFC)
    p.patch(0x49C, "3e3c1f14", bytes.fromhex("3e3c3ffb"))

    # 2. SAVE: "lea pc@(0x1691),a0 ; moveq #0,d1" -> "jsr sync_inventory" (it also does moveq #0,d1)
    p.patch(0x1476, "41fa02197200", jsr(SYNC_ADDR))

    # 3a. BUSACK waits, absolute form: movew $A11100,d0 ; btst #8,d0 ; bnes loop
    busack_abs = "303900a11100" "08000008" "66f4"
    for addr in (0x21DAA, 0x21DDA, 0x21E90, 0x21F10, 0x21FBA, 0x2237E):
        p.patch(addr, busack_abs, jsr(sym["wait_busack"]))
    # 3b. BUSACK waits, register form: movew a0@/a2@,d0 ; btst #8,d0 ; bnes loop
    p.patch(0x220EC, "3012" "08000008" "66f2", jsr(sym["wait_busack"]))
    for addr in (0x223FE, 0x2248A, 0x224F2, 0x2255A):
        p.patch(addr, "3010" "08000008" "66f2", jsr(sym["wait_busack"]))
    p.patch(0x22AB6, "3010" "08000008" "66f8", jsr(sym["wait_busack"]))
    # 3c. ready-flag waits (same 20-byte loop in two places)
    flag_loop = "4e92" "4a00" "660e" "48780001" "4e93" "588f" "4e92" "4a00" "67f2"
    p.patch(0xFD14, flag_loop, jmp(sym["flagwait_fd14"]))
    p.patch(0x23980, flag_loop, jmp(sym["flagwait_23980"]))

    # 4. header: ROM end and checksum
    p.write(0x1A4, AREA_END.to_bytes(4, "big"))
    p.write(0x18E, header_checksum(rom, AREA_END).to_bytes(2, "big"))
    return rom, p.touched


# --------------------------------------------------------------------------- verification

def verify(source: bytes, built: bytes, touched: list[tuple[int, int]]) -> int:
    """Every changed byte must be inside a patched range; header checksum must be valid."""
    if len(built) != ROM_SIZE:
        raise BuildError(f"output size is {len(built):#x}, expected {ROM_SIZE:#x}")
    changed = 0
    for i, (a, b) in enumerate(zip(source, built)):
        if a != b:
            changed += 1
            if not any(s <= i < e for s, e in touched):
                raise BuildError(f"byte {i:#x} changed outside the declared patch ranges")
    stored = int.from_bytes(built[0x18E:0x190], "big")
    end = int.from_bytes(built[0x1A4:0x1A8], "big")
    if stored != header_checksum(built, end):
        raise BuildError("header checksum does not match the output ROM")
    return changed


# --------------------------------------------------------------------------- main

def output_paths(output: Path) -> list[Path]:
    """The requested output plus its twin with the other ROM extension (.gen <-> .bin)."""
    twin = {".gen": ".bin", ".bin": ".gen"}.get(output.suffix.lower())
    return [output] + ([output.with_suffix(twin)] if twin else [])


def main() -> int:
    ap = argparse.ArgumentParser(description="Build the fixed Metal Gear MD v1.2 ROM.")
    ap.add_argument("source", nargs="?", type=Path,
                    help=f"your original v1.2 ROM (default: '{ORIGINAL_NAME}' in the current directory or next to this script)")
    ap.add_argument("-o", "--output", type=Path,
                    help=f"output ROM (default: '{FIXED_NAME}' next to the source ROM); "
                         "a twin with the other extension (.gen/.bin) is written too")
    ap.add_argument("--rebuild-patches", action="store_true", help="re-assemble patches/*.s even if the .bin files look current")
    ap.add_argument("--force", action="store_true", help="build even if the source SHA-256 is not the known v1.2 dump")
    args = ap.parse_args()

    try:
        if args.source is None:
            candidates = [Path.cwd() / ORIGINAL_NAME, HERE / ORIGINAL_NAME]
            args.source = next((c for c in candidates if c.is_file()), None)
            if args.source is None:
                raise BuildError(f"ROM not found; put '{ORIGINAL_NAME}' in the current directory or pass its path")
        if not args.source.is_file():
            raise BuildError(f"source ROM not found: {args.source}")
        if args.output is None:
            args.output = args.source.resolve().parent / FIXED_NAME
        outputs = output_paths(args.output)
        if args.source.resolve() in (o.resolve() for o in outputs):
            raise BuildError("output would overwrite the source ROM")
        source = args.source.read_bytes()
        if len(source) != ROM_SIZE:
            raise BuildError(f"source ROM is {len(source):#x} bytes, expected {ROM_SIZE:#x}")
        digest = hashlib.sha256(source).hexdigest()
        if digest != ORIGINAL_SHA256 and not args.force:
            raise BuildError(f"source SHA-256 {digest} is not the known v1.2 dump (use --force to try anyway; "
                             "patches still check their original bytes)")
        sym = ensure_patch_binaries(args.rebuild_patches)
        rom, touched = apply_patches(source, sym)
        changed = verify(source, bytes(rom), touched)
    except (BuildError, subprocess.CalledProcessError) as exc:
        print(f"error: {exc}", file=sys.stderr)
        return 1

    for out in outputs:
        out.write_bytes(rom)
        print(f"wrote {out}")
    print(f"  {changed} bytes changed, header checksum {int.from_bytes(rom[0x18E:0x190], 'big'):#06x}")
    print(f"  sha256 {hashlib.sha256(rom).hexdigest()}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
