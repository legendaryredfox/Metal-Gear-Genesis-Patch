/*
 * mgsim - minimal headless Mega Drive harness around the Musashi 68000 core, used by
 * tests/run_tests.py to check the Metal Gear Genesis Patch. NOT a general emulator:
 * the VDP, controller, Z80 and SRAM are small stubs.
 *
 * Usage:  SCRIPT=scenario.txt [SEED=n] [TAP=hexmask] [Z80DEAD=1] [RAMDUMP=file]  *         mgsim ROM.gen 0 [initial.srm]
 *
 * Environment:
 *   SEED     seed for the random power-on work-RAM contents (default 1)
 *   TAP      hex mask of pad buttons (active-low bits) pressed 6 frames out of every 40
 *   Z80DEAD  make the Z80 never answer (BUSACK stays 1, ready flag 0)
 *   RAMDUMP  write the final 64 KB of work RAM to this file
 * Scenario commands (one per line):
 *   run N               run N frames (VBlank interrupt each frame)
 *   poke ADDR HEXBYTES  write work RAM (ADDR is an offset in 0x0000-0xFFFF, a5 = 0x026C)
 *   peek ADDR LEN       print work RAM bytes
 *   call ADDR           call a 68000 routine and return (registers saved/restored)
 *   sram FILE           dump the SRAM (64 KB, odd bytes used) to FILE
 * Log lines flag odd-address word/long accesses and reads of work RAM never written.
 * Copyright for Musashi: Karl Stenerud (MIT). This file is only glue code.
 */
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include "m68k.h"
typedef uint8_t u8; typedef uint16_t u16; typedef uint32_t u32;
static u8 rom[0x400000]; static u8 ram[0x10000]; static u8 ramInit[0x10000]; static u8 sram[0x10000];
static int sramMap=0, sramWriteProt=0; static int romsz;
static long frame=0; static u32 curpc; static int phase;
static u16 pad1=0xFF; // active low bits: UDLRBC / SA
static int padsel1=0;
static long vcycle=0;
static int traceUninit=1, verbose=0;
FILE *logf;
static int callDone=0; void instr_hook(unsigned pc){ curpc=pc; if(pc==0x3FFFF0){ callDone=1; m68k_end_timeslice(); } }
static void L(const char*fmt,...);
#include <stdarg.h>
static void L(const char*fmt,...){va_list a;va_start(a,fmt);fprintf(logf,"[f%ld pc=%06X] ",frame,curpc);vfprintf(logf,fmt,a);fputc('\n',logf);va_end(a);}
static int vdpAccess=0;
u32 rd(u32 a,int sz){
  a&=0xFFFFFF;
  if(sz>1 && (a&1)) { L("ODD read%d @%06X",sz*8,a); }
  if(a<0x400000){
    if(a>=0x200000 && sramMap){ u32 o=a-0x200000; if(o<0x10000){ u32 v=0; for(int i=0;i<sz;i++){ u8 b=(((a+i)&1)? sram[(o+i)&0xFFFF]:0xFF); v=(v<<8)|b;} return v; } }
    u32 v=0; for(int i=0;i<sz;i++){ u32 p=(a+i); v=(v<<8)|(p<(u32)romsz?rom[p]:0xFF);} return v; }
  if(a>=0xE00000){ u32 v=0; for(int i=0;i<sz;i++){ u32 p=(a+i)&0xFFFF; if(traceUninit&&phase>0&&!ramInit[p]){ L("UNINIT RAM read @%06X (sz %d)",0xFF0000+p,sz); ramInit[p]=1; } v=(v<<8)|ram[p];} return v; }
  if(a>=0xA10000&&a<0xA10020){ u32 r=a&0x1F; if(r==1||r==0) return sz==1?0xA0:0xA0; // overseas NTSC, no TMSS? bit5 =? 
    if(r==2||r==3){ // data port 1
      u8 v = padsel1? (0x40|(pad1&0x3F)) : (0x00|((pad1>>6)&0x30)|(pad1&3)); return v;}
    return 0; }
  if(a>=0xA11100&&a<0xA11102){ if(getenv("Z80DEAD")) return sz==1?1:0x0100; return 0; }
  if(a>=0xC00000&&a<0xC00020){ u32 r=a&0x1F;
    if(r>=4&&r<8){ vcycle++; int vb = ((vcycle/50)&1)?0x08:0; return 0x3400|vb|0x200; }
    if(r>=8&&r<16){ static int hv=0; hv+=3; return hv&0xFFFF; }
    return 0; }
  if(a>=0xA00000&&a<0xA10000){ if(a==0xA00102) return getenv("Z80DEAD")?0:0x80; return 0;} // z80 stub: ready flag
  L("read from unmapped %06X sz%d",a,sz); return 0;
}
void wr(u32 a,u32 v,int sz){
  a&=0xFFFFFF;
  if(sz>1&&(a&1)) L("ODD write%d @%06X",sz*8,a);
  if(a<0x400000){
    if(a>=0x200000 && sramMap && !sramWriteProt){ u32 o=a-0x200000; if(o<0x10000){ for(int i=0;i<sz;i++){ if((a+i)&1) sram[(o+i)&0xFFFF]=(v>>(8*(sz-1-i)))&0xFF; } if(verbose) L("SRAM write %06X <= %X sz%d",a,v,sz);return;}}
    if(a>=0x200000&&!sramMap) L("write to ROM/SRAM space %06X=%X (SRAM not mapped)",a,v); else if(a>=0x200000) L("write to SRAM space while protected %06X",a);
    return; }
  if(a>=0xE00000){ for(int i=0;i<sz;i++){ u32 p=(a+i)&0xFFFF; ram[p]=(v>>(8*(sz-1-i)))&0xFF; ramInit[p]=1;} return; }
  if(a>=0xA130F0&&a<0xA13100){ if((a&1)){ if((a&0xF)==1){ sramMap=v&1; sramWriteProt=(v>>1)&1; L("MAPPER A130F1 <= %02X",v&0xFF);} else L("mapper %06X <= %X",a,v);} return; }
  if(a>=0xA10000&&a<0xA10020){ u32 r=a&0x1F; if(r==3){padsel1=(v>>6)&1;} return; }
  if(a>=0xA11100&&a<0xA11300) return;
  if(a>=0xA14000&&a<0xA14004) return;
  if(a>=0xA00000&&a<0xA10000) return;
  if(a>=0xC00000&&a<0xC00020) return;
  L("write to unmapped %06X=%X sz%d",a,v,sz);
}
unsigned m68k_read_memory_8(unsigned a){return rd(a,1);} unsigned m68k_read_memory_16(unsigned a){return rd(a,2);} unsigned m68k_read_memory_32(unsigned a){return rd(a,4);}
void m68k_write_memory_8(unsigned a,unsigned v){wr(a,v,1);} void m68k_write_memory_16(unsigned a,unsigned v){wr(a,v,2);} void m68k_write_memory_32(unsigned a,unsigned v){wr(a,v,4);}
unsigned m68k_read_disassembler_8(unsigned a){return a<(unsigned)romsz?rom[a]:0;}
unsigned m68k_read_disassembler_16(unsigned a){return (m68k_read_disassembler_8(a)<<8)|m68k_read_disassembler_8(a+1);}
unsigned m68k_read_disassembler_32(unsigned a){return (m68k_read_disassembler_16(a)<<16)|m68k_read_disassembler_16(a+2);}
int main(int argc,char**argv){
  FILE*f=fopen(argv[1],"rb"); romsz=fread(rom,1,sizeof rom,f); fclose(f);
  long frames=atol(argv[2]); logf=stdout;
  if(argc>3){ FILE*s=fopen(argv[3],"rb"); if(s){fread(sram,1,sizeof sram,s);fclose(s);} } else memset(sram,0xFF,sizeof sram);
  m68k_init(); m68k_set_cpu_type(M68K_CPU_TYPE_68000); m68k_pulse_reset();
  // randomize RAM like real HW
  srand(getenv("SEED")?atoi(getenv("SEED")):1); for(int i=0;i<0x10000;i++) ram[i]=rand();
  phase=0;
  FILE*sc=fopen(getenv("SCRIPT"),"r"); char line[512];
  while(fgets(line,sizeof line,sc)){
    char cmd[32]; unsigned a,b; if(sscanf(line,"%31s",cmd)<1) continue;
    if(!strcmp(cmd,"run")){ long n; sscanf(line,"run %ld",&n); for(long k=0;k<n;k++,frame++){ if(frame==1) phase=1;
        { const char*t=getenv("TAP"); pad1=0xFF; if(t && (frame%40)<6) pad1=0xFF & ~strtol(t,0,16); }
        m68k_execute(127000); m68k_set_irq(6); m68k_execute(10); m68k_set_irq(0);} }
    else if(!strcmp(cmd,"poke")){ char hx[512]; sscanf(line,"poke %x %511s",&a,hx); for(int k=0;hx[k*2]&&hx[k*2+1];k++){ unsigned v; sscanf(hx+k*2,"%2x",&v); ram[(a+k)&0xFFFF]=v; ramInit[(a+k)&0xFFFF]=1; } }
    else if(!strcmp(cmd,"peek")){ sscanf(line,"peek %x %u",&a,&b); printf("peek %06X:",a); for(unsigned k=0;k<b;k++) printf(" %02X",ram[(a+k)&0xFFFF]); printf("\n"); }
    else if(!strcmp(cmd,"call")){ sscanf(line,"call %x",&a); /* save regs */
        unsigned sp=m68k_get_reg(NULL,M68K_REG_SP), pc=m68k_get_reg(NULL,M68K_REG_PC), sr=m68k_get_reg(NULL,M68K_REG_SR); unsigned r[16]; for(int k=0;k<16;k++) r[k]=m68k_get_reg(NULL,M68K_REG_D0+k);
        sp-=4; ram[(sp)&0xFFFF]=0; ram[(sp+1)&0xFFFF]=0x3F; ram[(sp+2)&0xFFFF]=0xFF; ram[(sp+3)&0xFFFF]=0xF0; ramInit[(sp)&0xFFFF]=ramInit[(sp+1)&0xFFFF]=ramInit[(sp+2)&0xFFFF]=ramInit[(sp+3)&0xFFFF]=1;
        m68k_set_reg(M68K_REG_SP,sp); m68k_set_reg(M68K_REG_PC,a); callDone=0; int guard=0; while(!callDone&&guard++<400){ m68k_execute(100000);} printf("call %06X done=%d\n",a,callDone);
        for(int k=0;k<16;k++) m68k_set_reg(M68K_REG_D0+k,r[k]); m68k_set_reg(M68K_REG_PC,pc); m68k_set_reg(M68K_REG_SR,sr); }
    else if(!strcmp(cmd,"reg")){ char rn[8]; sscanf(line,"reg %7s %x",rn,&a); if(rn[0]=='d') m68k_set_reg(M68K_REG_D0+rn[1]-'0',a); }
    else if(!strcmp(cmd,"sram")){ char fn[256]; sscanf(line,"sram %255s",fn); FILE*o=fopen(fn,"wb"); fwrite(sram,1,sizeof sram,o); fclose(o);} 
  }
  L("done pc=%06X sr=%04X d2=%X d3=%X a7=%X", m68k_get_reg(NULL,M68K_REG_PC), m68k_get_reg(NULL,M68K_REG_SR), m68k_get_reg(NULL,M68K_REG_D2), m68k_get_reg(NULL,M68K_REG_D3), m68k_get_reg(NULL,M68K_REG_SP)); {u32 sp=m68k_get_reg(NULL,M68K_REG_SP); for(int i=0;i<10;i++) L("stack+%d = %08X",i*4,(u32)((ram[(sp+i*4)&0xFFFF]<<24)|(ram[(sp+i*4+1)&0xFFFF]<<16)|(ram[(sp+i*4+2)&0xFFFF]<<8)|ram[(sp+i*4+3)&0xFFFF]));}
  if(getenv("RAMDUMP")){FILE*o=fopen(getenv("RAMDUMP"),"wb");fwrite(ram,1,sizeof ram,o);fclose(o);} if(argc>4){ FILE*o=fopen(argv[4],"wb"); fwrite(sram,1,sizeof sram,o); fclose(o);} 
  return 0; }
