# Metal Gear Genesis Patcher 1.0.0

First release of the program. It replaces the old Python script and adds support for **Metal Gear v1.1**.

**Features**
- GUI (Compose Desktop, Kotlin Multiplatform) for Linux, macOS and Windows; interface in Portuguese, English, Russian and Chinese, with a Help screen (instructions, requirements, file list). Command-line mode with `--cli`.
- Supports both public dumps: v1.2 (`.gen`, 2 MB) and v1.1 (`.md`, 1.5 MB), recognized by SHA-256. Every patch checks the original bytes first; the result is verified before it is written (only declared bytes changed, header checksum valid).
- Fixes: SAVE keeps the current rank, ammo, equipment and weapons; Z80 waits get timeouts instead of hanging; full work-RAM clear at boot. On v1.1 the ROM self-checksum that corrupts the save signature of any patched ROM is also removed, and the RAM clear stops below the stack.
- The v1.2 output is byte-identical to the ROM already tested on a flashcart.

**Tests**: 65 Kotlin tests (SHA-256, patch tables, engine, CLI, screen state, UI, texts), and the Musashi behaviour suite (v1.2 10/10, v1.1 11/11). The fixed v1.1 boots to gameplay in BlastEm. **The v1.1 patch was not tested on real hardware.**

This release contains no ROMs. Use your own dumps. Java 21+ is needed for the `.jar`; the installers carry their own runtime.

---

# Metal Gear Genesis Patcher 1.0.0 (Português)

Primeira versão do programa. Substitui o antigo script em Python e passa a suportar o **Metal Gear v1.1**.

- Interface gráfica para Linux, macOS e Windows (português, inglês, russo e chinês, com tela de Ajuda) e modo `--cli`.
- Suporta v1.2 (`.gen`) e v1.1 (`.md`), reconhecidas pelo SHA-256. A saída da v1.2 é idêntica byte a byte à ROM já testada em flashcart.
- Correções: SAVE guarda os itens atuais, timeouts nas esperas do Z80, limpeza completa da RAM no boot; na v1.1 também remove o checksum da ROM que corrompe a assinatura do save.
- **O patch da v1.1 não foi testado em hardware real.** Esta release não contém ROMs. O `.jar` precisa de Java 21+.
