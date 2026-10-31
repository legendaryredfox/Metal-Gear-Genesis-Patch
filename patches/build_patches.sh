#!/bin/sh
# Assemble the patch routines. Outputs .bin files and z80_timeouts.sym ("name hexaddr").
# The -Ttext base below must equal Z80_ADDR in ../build_fixed_rom.py (the script checks it).
set -e
cd "$(dirname "$0")"
T=$(mktemp -d)
m68k-linux-gnu-as -m68000 -o $T/s.o sync_inventory.s
m68k-linux-gnu-objcopy -O binary -j .text $T/s.o sync_inventory.bin
m68k-linux-gnu-as -m68000 -o $T/z.o z80_timeouts.s
m68k-linux-gnu-ld -Ttext=0x1BD900 -e wait_busack -o $T/z.elf $T/z.o
m68k-linux-gnu-objcopy -O binary $T/z.elf z80_timeouts.bin
m68k-linux-gnu-nm $T/z.elf | awk '$2=="T"{print $3, $1}' > z80_timeouts.sym
rm -rf $T
