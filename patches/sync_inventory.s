| Metal Gear MD v1.2 - SRAM save fix.
| Called from the SAVE-to-slot routine (ROM 0x1476) before the checkpoint
| snapshot (a5+11328, 664 bytes) is copied into the slot.
| Copies the *live* inventory/rank over the snapshot, so a save keeps the items,
| ammo and rank the player has now, not those from the last checkpoint.
| Snapshot layout = serialisation table at ROM 0x4B5A (6 ranges, 664 bytes).
| Preserves d0 (slot number), sets d1 = 0 (the instruction it replaced).
        .text
sync_inventory:
        movem.l %d0/%d7/%a0-%a2,-(%sp)
        lea     %a5@(11328),%a1          | snapshot base
        move.b  %a5@(9054),%a1@(2)       | rank/class      live 0x235E -> snap 2
        lea     %a5@(9644),%a0           | ammo/counters   live 0x25AC (80 B) -> snap 64
        lea     %a1@(64),%a2
        move.w  #79,%d7
1:      move.b  %a0@+,%a2@+
        dbf     %d7,1b
        lea     %a5@(10060),%a0          | weapons+flags   live 0x274C (40 B) -> snap 320
        lea     %a1@(320),%a2
        move.w  #39,%d7
2:      move.b  %a0@+,%a2@+
        dbf     %d7,2b
        lea     %a5@(10108),%a0          | items+flags     live 0x277C (144 B) -> snap 360
        lea     %a1@(360),%a2
        move.w  #143,%d7
3:      move.b  %a0@+,%a2@+
        dbf     %d7,3b
        movem.l (%sp)+,%d0/%d7/%a0-%a2
        moveq   #0,%d1
        rts
