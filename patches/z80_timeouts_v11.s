| Metal Gear MD v1.1 - Z80 handshake timeouts.
| Same idea as z80_timeouts.s (v1.2), adapted to the older SGDK sound library of v1.1:
|   * 8 BUSACK waits on $A11100 (0xF9F8 0xFA28 0xFA7C 0xFAFC 0xFBA2 0xFD38 0xFDB6 0xFE1E)
|   * 5 ready-flag waits on $A00102 (0xFF18 0x10548 0x10E80 0x10F0C 0x10FAC), all with
|     a2 = 0xFD10 (read flag) and a3 = 0x11814 (delay) loaded right before the loop.
| If the Z80 never answers the original game hangs on a black screen; here every wait
| gives up after a bounded time and the game carries on (without sound).
|
| Z80_DEAD ($FF9000) is work RAM that the game never touches (its variables end at
| $FF8456 and the stack grows down from $FFE000). The patched boot RAM clear covers it,
| so it starts at 0. It is set when a BUSACK wait times out; while set, later waits make a
| single read and return at once. It is cleared again as soon as the Z80 grants the bus.
        .text
        .globl  wait_busack, flagwait
        .equ    Z80_DEAD, 0x00FF9000

| Replaces:  movew <$A11100>,d0 ; btst #8,d0 ; bnes <loop>   (any addr reg)
| Waits until BUSACK (bit 8 clear) or ~3.6 ms. Only d0 changes; a0/a2, which some
| callers still hold = $A11100, are untouched.
wait_busack:
        tst.b   Z80_DEAD
        bne     5f
        move.l  %d1,-(%sp)
        move.w  #0x0300,%d1
1:      move.w  0xA11100,%d0
        btst    #8,%d0
        dbeq    %d1,1b
        beq     2f
        st      Z80_DEAD               | timed out: no BUSACK
2:      move.l  (%sp)+,%d1
        rts
5:      move.w  0xA11100,%d0           | Z80 marked dead: one look only
        btst    #8,%d0
        bne     6f
        clr.b   Z80_DEAD               | it answered: back to normal
6:      rts

| Replaces the 20-byte ready-flag wait loop (jsr flagwait + 7 nops, so the call returns
| to the instruction that followed the loop). d2 is preserved.
| Gives up after 2500 polls (~0.5 s) or as soon as Z80_DEAD is set.
flagwait:
        move.l  %d2,-(%sp)
        move.w  #2500,%d2
        jsr     %a2@
        tstb    %d0
        bne     9f
1:      pea     1
        jsr     %a3@
        addq.l  #4,%sp
        tst.b   Z80_DEAD
        bne     9f
        jsr     %a2@
        tstb    %d0
        dbne    %d2,1b
9:      move.l  (%sp)+,%d2
        rts
