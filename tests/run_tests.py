#!/usr/bin/env python3
"""Regression tests for the Metal Gear Genesis Patch.

Builds a small headless 68000 harness (tests/sim/mgsim.c) against a Musashi checkout, then
runs the original and the fixed ROM through the same scenarios.

Needs: gcc, and a Musashi checkout:  git clone https://github.com/kstenerud/Musashi
Run:   python3 tests/run_tests.py --musashi /path/to/Musashi [--original ROM] [--fixed ROM]
Without --fixed it builds the fixed ROM first (build_fixed_rom.py). Your own original ROM is required.
"""
from __future__ import annotations

import argparse
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
A5 = 0x026C                      # game variables base inside work RAM (a5 = 0xFF026C)
ROM_NAME = "Metal Gear (World) (v1.2) (Aftermarket) (Unl).gen"
MUSASHI_FILES = ["m68k.h", "m68k_in.c", "m68kconf.h", "m68kcpu.c", "m68kcpu.h", "m68kdasm.c", "m68kmake.c",
                 "m68kfpu.c", "m68kmmu.h"]


def build_sim(musashi: Path, work: Path) -> Path:
    src = work / "musashi"
    src.mkdir()
    for name in MUSASHI_FILES:
        shutil.copy(musashi / name, src / name)
    shutil.copytree(musashi / "softfloat", src / "softfloat")
    conf = (src / "m68kconf.h").read_text()
    conf = re.sub(r"#define M68K_EMULATE_ADDRESS_ERROR\s+\S+", "#define M68K_EMULATE_ADDRESS_ERROR  M68K_OPT_ON", conf)
    conf = re.sub(r"#define M68K_INSTRUCTION_HOOK\s+\S+", "#define M68K_INSTRUCTION_HOOK       M68K_OPT_SPECIFY_HANDLER", conf)
    conf = re.sub(r"#define M68K_INSTRUCTION_CALLBACK\(.*\)[^\n]*", "#define M68K_INSTRUCTION_CALLBACK(pc) instr_hook(pc)", conf)
    (src / "m68kconf.h").write_text(conf)
    run = lambda *a, **k: subprocess.run(a, check=True, cwd=src, capture_output=True, text=True, **k)
    run("gcc", "-O2", "-o", "m68kmake", "m68kmake.c")
    run("./m68kmake")
    exe = work / "mgsim"
    run("gcc", "-O1", "-w", "-o", str(exe), str(ROOT / "tests/sim/mgsim.c"), "m68kcpu.c", "m68kops.c", "m68kdasm.c",
        "softfloat/softfloat.c", "-I.", "-lm")
    return exe


class Sim:
    def __init__(self, exe: Path, work: Path):
        self.exe, self.work, self.n = exe, work, 0

    def run(self, rom: Path, script: str, sram_in: Path | None = None, **env) -> list[str]:
        self.n += 1
        sc = self.work / f"scenario{self.n}.txt"
        sc.write_text(script)
        e = dict(os.environ, SCRIPT=str(sc), SEED=str(env.pop("seed", 1)), **{k: str(v) for k, v in env.items()})
        args = [str(self.exe), str(rom), "0"] + ([str(sram_in)] if sram_in else [])
        proc = subprocess.run(args, cwd=self.work, env=e, capture_output=True, text=True, timeout=600)
        if proc.returncode != 0:
            raise RuntimeError(f"mgsim failed ({proc.returncode}) on {rom}: {proc.stderr[-300:]}")
        return proc.stdout.splitlines()


def peeks(lines: list[str], expected: int) -> list[bytes]:
    out = [bytes(int(x, 16) for x in l.split(":", 1)[1].split()) for l in lines if l.startswith("peek ")]
    if len(out) != expected:
        raise RuntimeError(f"expected {expected} peek lines, got {len(out)}")
    return out


SAVE = """run 1500
poke {class_} 03
poke {ammo} 0123456789abcdef
poke {equip} 0100000705000009
poke {items} 0200000903000004
poke {mode} 02
poke {state} 02
poke {slot} 00
call 141a
sram {sram}
"""
LOAD = """run 100
poke {mode} 01
poke {state} 02
poke {slot} 0{n}
call 141a
call 4ad4
peek {class_} 1
peek {ammo} 8
peek {equip} 8
peek {items} 8
"""
ADDR = dict(class_="%x" % (A5 + 0x235E), ammo="%x" % (A5 + 0x25AC), equip="%x" % (A5 + 0x274C),
            items="%x" % (A5 + 0x277C), mode="%x" % (A5 + 0x28E0), state="%x" % (A5 + 0x28E1), slot="%x" % (A5 + 0x1E8D))
EXPECT = [bytes([3]), bytes.fromhex("0123456789abcdef"), bytes.fromhex("0100000705000009"), bytes.fromhex("0200000903000004")]


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--musashi", type=Path, default=os.environ.get("MUSASHI_DIR"), help="Musashi checkout (or env MUSASHI_DIR)")
    ap.add_argument("--original", type=Path, default=Path.cwd() / ROM_NAME, help=f"original v1.2 ROM (default: ./{ROM_NAME})")
    ap.add_argument("--fixed", type=Path)
    args = ap.parse_args()
    if not args.musashi or not Path(args.musashi, "m68kcpu.c").is_file():
        sys.exit("need a Musashi checkout: git clone https://github.com/kstenerud/Musashi ; then --musashi PATH")
    args.original = args.original.resolve()
    args.musashi = Path(args.musashi).resolve()
    if args.fixed:
        args.fixed = args.fixed.resolve()
    if not args.original.is_file():
        sys.exit(f"original ROM not found: {args.original} (use --original)")
    work = Path(tempfile.mkdtemp(prefix="mgtests-"))
    fixed = args.fixed
    if fixed is None:
        fixed = work / "fixed.gen"
        subprocess.run([sys.executable, str(ROOT / "build_fixed_rom.py"), str(args.original), "-o", str(fixed)], check=True)
    sim = Sim(build_sim(Path(args.musashi), work), work)
    orig = args.original
    results: list[tuple[str, bool, str]] = []

    def check(name: str, ok: bool, detail: str = "") -> None:
        results.append((name, ok, detail))
        print(("PASS " if ok else "FAIL ") + name + (f"  [{detail}]" if detail else ""))

    # 1. save -> reboot -> load keeps rank/ammo/equipment/weapons only with the fix
    for label, rom in (("original", orig), ("fixed", fixed)):
        sim.run(rom, SAVE.format(sram="sram_" + label, **ADDR), TAP="80")
        got = peeks(sim.run(rom, LOAD.format(n=0, **ADDR), work / ("sram_" + label), seed=3), 4)
        if label == "original":
            check("original ROM loses items on save/load (bug reproduced)", all(not any(g) for g in got), str([g.hex() for g in got]))
        else:
            check("fixed ROM restores rank, ammo, equipment and weapons", got == EXPECT, str([g.hex() for g in got]))

    # 2. two slots are independent, an empty slot loads zeros
    two = ("run 1500\n" + "".join(
        f"poke {ADDR['class_']} {c:02x}\npoke {ADDR['items']} {i}\npoke {ADDR['mode']} 02\npoke {ADDR['state']} 02\n"
        f"poke {ADDR['slot']} 0{s}\ncall 141a\n" for s, c, i in ((0, 2, "0a0000aa"), (1, 4, "0b0000bb"))) + "sram sram_two\n")
    sim.run(fixed, two, TAP="80")
    res = []
    for n in (0, 1, 2):
        g = peeks(sim.run(fixed, LOAD.format(n=n, **ADDR), work / "sram_two", seed=7), 4)
        res.append((g[0], g[3][:4]))
    check("fixed ROM keeps slots independent", res == [(bytes([2]), bytes.fromhex("0a0000aa")),
          (bytes([4]), bytes.fromhex("0b0000bb")), (bytes([0]), bytes(4))], str([(a.hex(), b.hex()) for a, b in res]))

    # 3. Z80 that never answers: original hangs, fixed reaches the game (mode var a5+0x21C6 == 1)
    mode = "%x" % (A5 + 0x21C6)
    flag = "fc00"
    probe = f"run 3000\npeek {mode} 1\npeek {flag} 1\n"
    for label, rom in (("original", orig), ("fixed", fixed)):
        p = peeks(sim.run(rom, probe, TAP="80", Z80DEAD=1), 2)
        if label == "original":
            check("original ROM hangs forever with a dead Z80", p[0] == b"\x00", p[0].hex())
        else:
            check("fixed ROM survives a dead Z80 and sets Z80_DEAD", p[0] == b"\x01" and p[1] == b"\xff", f"{p[0].hex()} {p[1].hex()}")

    # 4. healthy Z80: same game state as the original and the dead flag stays clear
    po = peeks(sim.run(orig, probe, TAP="80"), 2)
    pf = peeks(sim.run(fixed, probe, TAP="80"), 2)
    check("healthy Z80: same game mode as the original, Z80_DEAD clear", po[0] == pf[0] == b"\x01" and pf[1] == b"\x00",
          f"{po[0].hex()} {pf[0].hex()} {pf[1].hex()}")

    # 5. no odd-address accesses, no uninitialised work-RAM reads in the fixed ROM
    log = sim.run(fixed, "run 3000\n", TAP="80", seed=2)
    bad = [l for l in log if "ODD" in l or "UNINIT" in l]
    check("fixed ROM: no odd-address access, no uninitialised RAM read", not bad, "; ".join(bad[:2]))

    # 6. real-hardware rules (bus ownership, Z80 reset pulse, DMA 128 KB boundary), both ROMs, two pad inputs
    for label, rom in (("original", orig), ("fixed", fixed)):
        viol = []
        for tap in ("80", "10"):
            viol += [l.split("]", 1)[1].strip() for l in sim.run(rom, "run 3000\n", TAP=tap, seed=4) if "HWVIOL" in l]
        check(f"{label} ROM: no Z80/YM access without BUSREQ, no short Z80 reset, no DMA across 128 KB",
              not viol, "; ".join(sorted(set(viol))[:2]))

    failed = [r for r in results if not r[1]]
    print(f"\n{len(results) - len(failed)}/{len(results)} passed")
    shutil.rmtree(work, ignore_errors=True)
    return 1 if failed else 0


if __name__ == "__main__":
    sys.exit(main())
