| Metal Gear MD v1.2 - Z80 handshake timeouts.
| The original waits for the Z80 forever (BUSACK on $A11100 and the ready flag
| on $A00102). If the Z80 never answers, the game hangs on a black screen.
| These routines give up after a bounded time and carry on (without sound).
|
| Z80_DEAD ($FFFC00, RAM) is cleared by the boot RAM clear. It is set when a BUSACK
| wait times out; while set, later waits make a single read and return at once.
| It is cleared again as soon as the Z80 grants the bus.
        .text
        .globl  wait_busack, flagwait_fd14, flagwait_23980
        .equ    Z80_DEAD, 0x00FFFC00

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

| Replaces the 20-byte ready-flag wait at 0xFD14 (continues at 0xFD28).
| a2 = read-flag routine (0x223D6), a3 = delay routine (0x14B4A), d2 free.
| Gives up after 2500 polls (~0.5 s) or as soon as Z80_DEAD is set.
flagwait_fd14:
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
9:      jmp     0xFD28

| Same loop at 0x23980 (continues at 0x23994).
flagwait_23980:
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
9:      jmp     0x23994
