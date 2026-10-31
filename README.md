# Metal Gear Genesis Patch

**Idiomas / Языки / Languages:** [Português](#português) · [Русский](#русский) · [English](#english)

☕ [Buy Me a Coffee](https://buymeacoffee.com/legendaryredfox)

---

<a id="português"></a>

# Português

Correções para a ROM **Metal Gear (World) (v1.2) (Aftermarket) (Unl)** de Mega Drive/Genesis (port de H0ffman, Ice Dragon Dee,
Inglebard, Toni Galvez e 5customs do jogo da Konami), voltadas para uso em **hardware real** (cartuchos repro com bateria e flashcarts).

> Este repositório **não contém ROMs**. Ele traz scripts que aplicam o patch na *sua* cópia da ROM v1.2
> (SHA-256 `1707f5534028cd4a3d049a552b1567fc7a3100598b450a976ed0690754be8f6a`). Projeto não oficial, sem ligação com a Konami ou com os autores do port.

## O que muda

| # | Onde | O quê |
|---|------|-------|
| 1 | 0x1476 → 0x1BD880 | **SAVE** passa a gravar rank, munição, equipamento e armas **atuais**. Antes só gravava o snapshot do último checkpoint, então itens/rank obtidos depois dele (ou desde o início) se perdiam |
| 2 | 12 pontos + 0xFD14 + 0x23980 → 0x1BD900 | Esperas do Z80 (BUSACK em `0xA11100` e flag de pronto em `0xA00102`) ganham **timeout**; antes eram infinitas e podiam travar o jogo na tela preta |
| 3 | 0x049E | Clear da RAM no boot cobre `0xFF0000–0xFFFFEF` (antes só até `0xFF7C53`). Correção defensiva |
| 4 | header 0x18E / 0x1A4 | Checksum recalculado; ROM end = 0x1BD9FF |

Total: 336 bytes alterados. O formato do SRAM não muda, então saves da ROM original continuam carregando.

### Detalhes técnicos

- **Save:** o jogo mantém um snapshot de 664 bytes (tabela `(offset, tamanho)` em 0x4B5A, 6 faixas de RAM) atualizado só em salas-checkpoint
  (0x4A9E → 0x4ADE). O SAVE (0x146E) copiava esse snapshot para o slot SRAM (5 slots de 664 bytes em `0x200011`, bytes ímpares, assinatura `MGSV`).
  A rotina nova (`patches/sync_inventory.s`) copia antes: rank (a5+0x235E), munição (a5+0x25AC, 80 B), itens+flags (a5+0x277C, 144 B) e armas+flags
  (a5+0x274C, 40 B). Posição/sala continuam as do checkpoint (`a5 = 0xFF026C`).
- **Z80:** os 12 laços de BUSACK (0x21DAA, 0x21DDA, 0x21E90, 0x21F10, 0x21FBA, 0x2237E, 0x220EC, 0x223FE, 0x2248A, 0x224F2, 0x2255A, 0x22AB6) esperam ~3,6 ms; os dois
  laços do flag de pronto (0xFD14, 0x23980), ~0,5 s. No estouro, `Z80_DEAD` (`0xFFFC00`) é marcado e as esperas seguintes olham uma vez só; se o Z80 volta a responder, o flag é limpo.
  O jogo segue sem som em vez de travar. Com Z80 normal o comportamento é idêntico ao original (`patches/z80_timeouts.s`).
- **RAM:** o driver de som lê `0xFF86AA` e `0xFF85D0` sem inicializar. A análise (Ghidra 12.1.3 via REA) mostra que só um lixo entre 1 e 5 muda o fluxo, então é causa **pouco provável**
  de travamento; o clear completo é barato e foi mantido.

## Como usar

1. Coloque sua ROM `Metal Gear (World) (v1.2) (Aftermarket) (Unl).gen` na pasta do projeto (ou passe o caminho).
2. `python3 build_fixed_rom.py`  → gera `... [fixed].gen` ao lado da original.

Opções: `build_fixed_rom.py [ROM] [-o SAÍDA] [--rebuild-patches] [--force]`. O script confere o SHA-256 da ROM e os bytes originais de cada patch (só funciona na v1.2 intacta)
e, depois de construir, verifica que todo byte alterado pertence a um patch e que o checksum do header está certo.

**Requisitos:** Python 3 (apenas biblioteca padrão; `requirements.txt` não lista pacotes pip). Para remontar `patches/*.s` (feito sozinho se um `.s` for mais novo que o `.bin`):
`sudo apt install binutils-m68k-linux-gnu`.

## Testes

`python3 tests/run_tests.py --musashi /caminho/Musashi --original "<sua ROM>.gen"` (precisa de `gcc` e de um checkout do [Musashi](https://github.com/kstenerud/Musashi)).
Monta um simulador 68000 mínimo (`tests/sim/mgsim.c`, VDP/SRAM/Z80 simplificados) e roda a ROM original e a corrigida nos mesmos cenários: perda e restauração de itens/rank no
save→reboot→load, slots independentes, Z80 que nunca responde (a original trava, a corrigida segue), paridade com Z80 normal e ausência de acessos ímpares/RAM não inicializada. Resultado atual: 7/7.

Verificado também (manualmente, sob xvfb): BlastEm (regiões U, J e E/PAL, RAM inicial aleatória), RetroArch com o core BlastEm e mednafen 1.29 iniciam e chegam ao jogo; o áudio do BlastEm é equivalente
na original e na corrigida; no RetroArch, itens (Ration, Card 1, binóculo) foram gravados no `.srm` e restaurados pelo LOAD.

## Limitações

- **Cartucho real não testado.** O travamento em hardware não foi reproduzido em nenhum emulador; os timeouts do Z80 são a melhor hipótese, não uma prova.
- Salvar pelo menu de pause não foi automatizado; foi feito à mão no RetroArch.
- O laço em 0x1019A (espera o Z80 liberar `0xA00160` bit 0) não foi alterado.
- Continuar depois de morrer (0x3A92 → 0x4AD4) ainda restaura itens do último checkpoint.

## Estrutura

```
build_fixed_rom.py     aplica os patches e verifica o resultado
patches/               rotinas 68000 (.s), binários montados (.bin/.sym) e build_patches.sh
tests/run_tests.py     testes de regressão;  tests/sim/mgsim.c  simulador mínimo (Musashi)
requirements.txt       dependências (nenhum pacote pip)
```

## Apoie o projeto

Se este patch foi útil, você pode me apoiar em [buymeacoffee.com/legendaryredfox](https://buymeacoffee.com/legendaryredfox).

---

<a id="русский"></a>

# Русский

Исправления для ROM **Metal Gear (World) (v1.2) (Aftermarket) (Unl)** для Mega Drive/Genesis (порт игры Konami от H0ffman, Ice Dragon Dee,
Inglebard, Toni Galvez и 5customs), рассчитанные на **реальное железо** (репро-картриджи с батарейкой и флеш-картриджи).

> В репозитории **нет ROM**. Здесь скрипты, которые патчат *вашу* копию ROM v1.2
> (SHA-256 `1707f5534028cd4a3d049a552b1567fc7a3100598b450a976ed0690754be8f6a`). Неофициальный проект, не связан с Konami и авторами порта.

## Что меняется

| # | Где | Что |
|---|-----|-----|
| 1 | 0x1476 → 0x1BD880 | **SAVE** теперь записывает **текущие** звание, патроны, снаряжение и оружие. Раньше записывался только снимок последнего чекпоинта, поэтому предметы/звание, полученные после него (или с начала игры), терялись |
| 2 | 12 мест + 0xFD14 + 0x23980 → 0x1BD900 | Ожидания Z80 (BUSACK по `0xA11100` и флаг готовности по `0xA00102`) получили **тайм-аут**; раньше были бесконечными и могли зависнуть на чёрном экране |
| 3 | 0x049E | Очистка RAM при загрузке охватывает `0xFF0000–0xFFFFEF` (раньше только до `0xFF7C53`). Защитное исправление |
| 4 | заголовок 0x18E / 0x1A4 | Контрольная сумма пересчитана; ROM end = 0x1BD9FF |

Итого изменено 336 байт. Формат SRAM не меняется, поэтому сохранения оригинального ROM продолжают загружаться.

### Технические детали

- **Сохранение:** игра держит снимок размером 664 байта (таблица `(смещение, размер)` в 0x4B5A, 6 диапазонов RAM), который обновляется только в комнатах-чекпоинтах
  (0x4A9E → 0x4ADE). SAVE (0x146E) копировал этот снимок в слот SRAM (5 слотов по 664 байта с `0x200011`, нечётные байты, подпись `MGSV`).
  Новая процедура (`patches/sync_inventory.s`) сначала копирует: звание (a5+0x235E), патроны (a5+0x25AC, 80 Б), предметы+флаги (a5+0x277C, 144 Б) и оружие+флаги
  (a5+0x274C, 40 Б). Позиция/комната по-прежнему берутся из чекпоинта (`a5 = 0xFF026C`).
- **Z80:** 12 циклов ожидания BUSACK (0x21DAA, 0x21DDA, 0x21E90, 0x21F10, 0x21FBA, 0x2237E, 0x220EC, 0x223FE, 0x2248A, 0x224F2, 0x2255A, 0x22AB6) ждут ~3,6 мс; два
  цикла флага готовности (0xFD14, 0x23980) — ~0,5 с. По тайм-ауту выставляется `Z80_DEAD` (`0xFFFC00`), и следующие ожидания делают одно чтение; если Z80 снова отвечает, флаг сбрасывается.
  Игра продолжает работу без звука вместо зависания. При исправном Z80 поведение идентично оригиналу (`patches/z80_timeouts.s`).
- **RAM:** звуковой драйвер читает `0xFF86AA` и `0xFF85D0` без инициализации. Анализ (Ghidra 12.1.3 через REA) показывает, что поток меняется только при мусоре 1–5, поэтому это
  **маловероятная** причина зависания; полная очистка дешёвая и оставлена.

## Как использовать

1. Положите свой ROM `Metal Gear (World) (v1.2) (Aftermarket) (Unl).gen` в папку проекта (или укажите путь).
2. `python3 build_fixed_rom.py`  → создаёт `... [fixed].gen` рядом с оригиналом.

Опции: `build_fixed_rom.py [ROM] [-o ВЫХОД] [--rebuild-patches] [--force]`. Скрипт проверяет SHA-256 ROM и исходные байты каждого патча (работает только на нетронутой v1.2),
а после сборки проверяет, что каждый изменённый байт принадлежит патчу и контрольная сумма заголовка верна.

**Требования:** Python 3 (только стандартная библиотека; `requirements.txt` не содержит pip-пакетов). Для пересборки `patches/*.s` (запускается сама, если `.s` новее `.bin`):
`sudo apt install binutils-m68k-linux-gnu`.

## Тесты

`python3 tests/run_tests.py --musashi /путь/Musashi --original "<ваш ROM>.gen"` (нужны `gcc` и клон [Musashi](https://github.com/kstenerud/Musashi)).
Собирается минимальный симулятор 68000 (`tests/sim/mgsim.c`, упрощённые VDP/SRAM/Z80), и оригинальный и исправленный ROM прогоняются по одним сценариям: потеря и восстановление
предметов/звания при сохранении→перезагрузке→загрузке, независимые слоты, Z80, который никогда не отвечает (оригинал виснет, исправленный продолжает), совпадение с оригиналом при
исправном Z80 и отсутствие нечётных обращений/неинициализированной RAM. Текущий результат: 7/7.

Также проверено (вручную, под xvfb): BlastEm (регионы U, J и E/PAL, случайная начальная RAM), RetroArch с ядром BlastEm и mednafen 1.29 запускаются и доходят до игры; звук BlastEm
эквивалентен у оригинала и исправленного ROM; в RetroArch предметы (Ration, Card 1, бинокль) записались в `.srm` и восстановились через LOAD.

## Ограничения

- **Реальный картридж не тестировался.** Зависание на железе не воспроизведено ни в одном эмуляторе; тайм-ауты Z80 — лучшая гипотеза, а не доказательство.
- Сохранение через меню паузы не автоматизировано; выполнено вручную в RetroArch.
- Цикл в 0x1019A (ждёт, пока Z80 освободит `0xA00160` бит 0) не изменён.
- Продолжение после смерти (0x3A92 → 0x4AD4) по-прежнему восстанавливает предметы с последнего чекпоинта.

## Структура

```
build_fixed_rom.py     применяет патчи и проверяет результат
patches/               процедуры 68000 (.s), собранные файлы (.bin/.sym) и build_patches.sh
tests/run_tests.py     регрессионные тесты;  tests/sim/mgsim.c  минимальный симулятор (Musashi)
requirements.txt       зависимости (pip-пакетов нет)
```

## Поддержать проект

Если патч оказался полезен, можно поддержать меня на [buymeacoffee.com/legendaryredfox](https://buymeacoffee.com/legendaryredfox).

---

<a id="english"></a>

# English

Fixes for the Mega Drive/Genesis ROM **Metal Gear (World) (v1.2) (Aftermarket) (Unl)** (port of Konami's game by H0ffman, Ice Dragon Dee,
Inglebard, Toni Galvez and 5customs), aimed at **real hardware** (battery-backed repro carts and flashcarts).

> This repository **contains no ROMs**. It ships scripts that patch *your own* copy of the v1.2 ROM
> (SHA-256 `1707f5534028cd4a3d049a552b1567fc7a3100598b450a976ed0690754be8f6a`). Unofficial project, not affiliated with Konami or the port's authors.

## What changes

| # | Where | What |
|---|-------|------|
| 1 | 0x1476 → 0x1BD880 | **SAVE** now writes the **current** rank, ammo, equipment and weapons. It used to write only the last checkpoint's snapshot, so items/rank obtained after it (or since the start) were lost |
| 2 | 12 sites + 0xFD14 + 0x23980 → 0x1BD900 | Z80 waits (BUSACK on `0xA11100` and the ready flag on `0xA00102`) get a **timeout**; they used to be infinite and could hang the game on a black screen |
| 3 | 0x049E | Boot RAM clear covers `0xFF0000–0xFFFFEF` (was only up to `0xFF7C53`). Defensive fix |
| 4 | header 0x18E / 0x1A4 | Checksum recalculated; ROM end = 0x1BD9FF |

336 bytes changed in total. The SRAM format is unchanged, so saves made with the original ROM still load.

### Technical details

- **Save:** the game keeps a 664-byte snapshot (`(offset, size)` table at 0x4B5A, 6 RAM ranges) refreshed only in checkpoint rooms
  (0x4A9E → 0x4ADE). SAVE (0x146E) copied that snapshot into the SRAM slot (5 slots of 664 bytes at `0x200011`, odd bytes, signature `MGSV`).
  The new routine (`patches/sync_inventory.s`) first copies: rank (a5+0x235E), ammo (a5+0x25AC, 80 B), items+flags (a5+0x277C, 144 B) and weapons+flags
  (a5+0x274C, 40 B). Position/room still come from the checkpoint (`a5 = 0xFF026C`).
- **Z80:** the 12 BUSACK loops (0x21DAA, 0x21DDA, 0x21E90, 0x21F10, 0x21FBA, 0x2237E, 0x220EC, 0x223FE, 0x2248A, 0x224F2, 0x2255A, 0x22AB6) wait ~3.6 ms; the two
  ready-flag loops (0xFD14, 0x23980) ~0.5 s. On timeout `Z80_DEAD` (`0xFFFC00`) is set and later waits only look once; if the Z80 answers again the flag is cleared.
  The game carries on without sound instead of hanging. With a healthy Z80 behaviour is identical to the original (`patches/z80_timeouts.s`).
- **RAM:** the sound driver reads `0xFF86AA` and `0xFF85D0` uninitialised. Analysis (Ghidra 12.1.3 via REA) shows only garbage between 1 and 5 changes the flow, so this is an **unlikely**
  cause of a hang; the full clear is cheap and was kept.

## Usage

1. Put your `Metal Gear (World) (v1.2) (Aftermarket) (Unl).gen` in the project folder (or pass its path).
2. `python3 build_fixed_rom.py`  → writes `... [fixed].gen` next to the original.

Options: `build_fixed_rom.py [ROM] [-o OUTPUT] [--rebuild-patches] [--force]`. The script checks the ROM's SHA-256 and the original bytes of every patch (so it only works on an untouched v1.2),
and after building verifies that every changed byte belongs to a patch and that the header checksum is valid.

**Requirements:** Python 3 (standard library only; `requirements.txt` lists no pip packages). To re-assemble `patches/*.s` (done automatically if a `.s` is newer than its `.bin`):
`sudo apt install binutils-m68k-linux-gnu`.

## Tests

`python3 tests/run_tests.py --musashi /path/to/Musashi --original "<your ROM>.gen"` (needs `gcc` and a checkout of [Musashi](https://github.com/kstenerud/Musashi)).
It builds a minimal 68000 simulator (`tests/sim/mgsim.c`, simplified VDP/SRAM/Z80) and runs the original and the fixed ROM through the same scenarios: loss and restoration of items/rank on
save→reboot→load, independent slots, a Z80 that never answers (the original hangs, the fixed ROM carries on), parity with a healthy Z80, and no odd-address accesses/uninitialised RAM. Current result: 7/7.

Also checked (manually, under xvfb): BlastEm (regions U, J and E/PAL, random initial RAM), RetroArch with the BlastEm core and mednafen 1.29 boot and reach gameplay; BlastEm's audio is equivalent
for the original and the fixed ROM; in RetroArch, items (Ration, Card 1, binoculars) were written to the `.srm` and restored by LOAD.

## Limitations

- **Real cartridge not tested.** The hardware hang was not reproduced in any emulator; the Z80 timeouts are the best hypothesis, not a proof.
- Saving through the pause menu was not automated; it was done by hand in RetroArch.
- The loop at 0x1019A (waits for the Z80 to release `0xA00160` bit 0) was not changed.
- Continuing after death (0x3A92 → 0x4AD4) still restores items from the last checkpoint.

## Layout

```
build_fixed_rom.py     applies the patches and verifies the result
patches/               68000 routines (.s), assembled binaries (.bin/.sym) and build_patches.sh
tests/run_tests.py     regression tests;  tests/sim/mgsim.c  minimal simulator (Musashi)
requirements.txt       dependencies (no pip packages)
```

## Support

If this patch helped you, you can support me at [buymeacoffee.com/legendaryredfox](https://buymeacoffee.com/legendaryredfox).
