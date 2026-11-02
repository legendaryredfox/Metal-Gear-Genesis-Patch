# Metal Gear Genesis Patcher

**Idiomas / Языки / 语言 / Languages:** [Português](#português) · [Русский](#русский) · [简体中文](#简体中文) · [English](#english)

☕ [Buy Me a Coffee](https://buymeacoffee.com/legendaryredfox)

---

<a id="português"></a>

# Português

Programa com interface gráfica (e modo linha de comando) que aplica correções para **hardware real** (cartuchos repro com bateria e flashcarts) no port de **Metal Gear** para Mega Drive/Genesis
(H0ffman, Ice Dragon Dee, Inglebard, Toni Galvez e 5customs, a partir do jogo da Konami). Feito em **Kotlin Multiplatform** (Compose Desktop): roda em **Linux, macOS e Windows**.

> Este repositório **não contém ROMs**. O programa corrige a *sua* cópia. Projeto não oficial, sem ligação com a Konami ou com os autores do port.

| Versão | Arquivo usual | Tamanho | SHA-256 |
|--------|---------------|---------|---------|
| v1.2 | `Metal Gear (World) (v1.2) (Aftermarket) (Unl).gen` | 2 MB | `1707f5534028cd4a3d049a552b1567fc7a3100598b450a976ed0690754be8f6a` |
| v1.1 | `Metal Gear (v1.1).md` | 1,5 MB | `8073e3f1e966d8f6ab5b97f4d7e96208dd32dbea01997c3a4b5e2c5195b06f8b` |

## Como usar

**Interface gráfica:** `java -jar MetalGearPatcher-<so>-<arq>-<versão>.jar` (Java 21+), ou o instalador nativo (`.deb`, `.dmg`, `.msi`). Escolha a ROM (ou solte o arquivo na janela), marque as correções e clique em *Aplicar correções*.
O programa reconhece a versão pelo SHA-256, confere os bytes originais de cada patch e, depois de montar, verifica que todo byte alterado pertence a um patch e que o checksum do cabeçalho está certo. Grava `... [fixed].gen` e `... [fixed].bin` (conteúdo idêntico; use a extensão que o menu do seu flashcart lista). O idioma da interface (português, inglês, russo e chinês) segue o do sistema e pode ser trocado na janela. O botão *Ajuda* traz instruções gerais, os requisitos (bibliotecas e programas) e a lista de arquivos.

**Linha de comando:** `java -jar MetalGearPatcher-*.jar --cli ROM [-o SAÍDA] [--only save,z80,ram] [--force] [--check]`. Código de saída 0 = ok, 1 = erro, 2 = uso incorreto. `--force` aceita um dump modificado (hash diferente) se todos os bytes verificados ainda baterem.

## O que muda

**v1.2** (336 bytes alterados):

| # | Onde | O quê |
|---|------|-------|
| 1 | 0x1476 → 0x1BD880 | **SAVE** passa a gravar rank, munição, equipamento e armas **atuais**. Antes só gravava o snapshot do último checkpoint, então itens/rank obtidos depois dele (ou desde o início) se perdiam |
| 2 | 12 pontos + 0xFD14 + 0x23980 → 0x1BD900 | Esperas do Z80 (BUSACK em `0xA11100` e flag de pronto em `0xA00102`) ganham **timeout**; antes eram infinitas e podiam travar o jogo na tela preta |
| 3 | 0x049C | Clear da RAM no boot cobre `0xFF0000–0xFFFFEF` (antes só até `0xFF7C53`) |
| 4 | header 0x18E / 0x1A4 | Checksum recalculado; ROM end = 0x1BD9FF |

**v1.1** (331 bytes alterados; o código é o mesmo da v1.2 deslocado, com uma biblioteca de som SGDK mais antiga):

| # | Onde | O quê |
|---|------|-------|
| 1 | 0x14AC → 0x13FE80 | Mesma correção do **SAVE** (mesmo formato de SRAM, os saves antigos continuam carregando) |
| 2 | 8 esperas BUSACK (0xF9F8, 0xFA28, 0xFA7C, 0xFAFC, 0xFBA2, 0xFD38, 0xFDB6, 0xFE1E) + 5 esperas da flag de pronto (0xFF18, 0x10548, 0x10E80, 0x10F0C, 0x10FAC) → 0x13FF00 | **Timeouts** do Z80; `Z80_DEAD` fica em `0xFF9000` |
| 3 | 0x04A0 | Clear da RAM no boot cobre `0xFF0000–0xFFDFEF`. **Não** pode ir até o topo como na v1.2: a pilha da v1.1 começa em `0xFFE000` e guarda o endereço de retorno da rotina de clear |
| 4 | 0x0486 e 0x1630 | **Remove o checksum da ROM** (obrigatório, veja abaixo) |
| 5 | header 0x18E / 0x1A4 | Checksum recalculado; ROM end = 0x13FFFF |

### Detalhes técnicos

- **Save:** o jogo mantém um snapshot de 664 bytes (tabela `(offset, tamanho)`, 6 faixas de RAM) atualizado só em salas-checkpoint. O SAVE copiava esse snapshot para o slot SRAM (5 slots de 664 bytes em `0x200011`, bytes ímpares, assinatura `MGSV`).
  A rotina nova (`patches/sync_inventory.s`) copia antes: rank (a5+0x235E), munição (a5+0x25AC, 80 B), itens+flags (a5+0x277C, 144 B) e armas+flags (a5+0x274C, 40 B). Posição/sala continuam as do checkpoint (`a5 = 0xFF026C`). É a mesma rotina nas duas versões.
- **Z80:** os laços de BUSACK esperam ~3,6 ms; os da flag de pronto, ~0,5 s. No estouro, `Z80_DEAD` é marcado e as esperas seguintes olham uma vez só; se o Z80 volta a responder, o flag é limpo. O jogo segue sem som em vez de travar. Com Z80 normal o comportamento é idêntico ao original (`patches/z80_timeouts.s` e `patches/z80_timeouts_v11.s`). Os timeouts dependem do clear de RAM (o flag precisa começar em zero), por isso o programa liga os dois juntos.
- **Checksum da ROM (só v1.1):** a rotina em `0x93FC`/`0x798` soma a ROM de `0x474` até `0xF96C` e **soma a diferença à assinatura `MGSV`** do bloco de save. Em ROM íntegra a diferença é 0. Se qualquer byte dessa faixa muda (e o SAVE e o clear de RAM estão nela), o SAVE grava uma assinatura inválida e o LOAD passa a ignorar o save. A v1.2 removeu essa checagem; aqui as duas chamadas viram NOPs. O simulador reproduz o efeito: um único byte alterado na v1.1 original dá a assinatura `MGSU`.
- **RAM:** na v1.1 as variáveis do driver de som e do nível de interrupção ficam em `0xFF7C5E` e `0xFF7D3A`, acima do que o boot original limpava (`0xFF7C53`); o simulador mostra leituras de RAM não inicializada ali no quadro 11 da ROM original e nenhuma na corrigida. Na v1.2 o driver lê `0xFF86AA` e `0xFF85D0`.

### Outras melhorias verificadas na v1.1

Feitas: as listadas acima. Verificadas e **não** feitas (detalhes, endereços e evidências em [docs/v1.1-analise.md](docs/v1.1-analise.md)):

- 19 leituras de `0xA00160` (fila de comandos do Z80) sem timeout, em formatos diferentes; a v1.2 também as deixou. Exigem um patch por ponto.
- Variáveis do SGDK (semente do `random()`, contador de ticks, contexto de multitarefa) estão em endereços de ROM (`0x13FE10–0x13FE5A`) e escrever nelas não faz nada. O código que as usa não é chamado em lugar nenhum, então é inofensivo.
- Diferenças de lógica entre v1.1 e v1.2 fora do driver de som: um menu com 4 opções vira 5 (`0xF68`), e uma flag em `a5@(0)` passa a valer 1 ao fim de uma rotina (`0x6A54`). A intenção não está clara; não foram portadas.

## Compilar e testar

Requisitos: JDK 21+ (o Gradle baixa o resto). Para remontar `patches/*.s`: `sudo apt install binutils-m68k-linux-gnu`.

```
./gradlew :app:run                          abre a interface
./gradlew :app:desktopTest                  testes unitários e de interface
MG_ROM_DIR=/pasta/com/as/roms ./gradlew :app:desktopTest   inclui os testes com as ROMs reais (pulados sem elas)
./gradlew :app:packageUberJarForCurrentOS   jar único para o sistema atual
./gradlew :app:packageDistributionForCurrentOS   instalador nativo (.deb/.dmg/.msi) do sistema atual
./patches/build_patches.sh                  remonta as rotinas 68000 e regenera PatchBlobs.kt
```

**Testes em Kotlin** (65): SHA-256 (vetores e conferência com o JDK), tabelas de patch (endereços, sobreposição, destinos de jsr/jmp), motor (identificação, build, subconjuntos, recusas), estado da tela, CLI, textos nos quatro idiomas e a interface com Compose (escolher ROM, aplicar, trocar idioma, ajuda). Com as ROMs reais: a saída da v1.2 é **idêntica byte a byte** à ROM já testada em flashcart (SHA-256 `0c52b1e2…c646`, 336 bytes), e a v1.1 gera o SHA-256 `be4ea50d…07e5`. Os testes com ROM sintética não detectam um endereço errado na tabela (ela é montada a partir da própria tabela); só os testes com ROM real detectam.

**Comportamento da ROM** (`tests/run_tests.py`, precisa de `gcc` e de um checkout do [Musashi](https://github.com/kstenerud/Musashi)):
`python3 tests/run_tests.py --musashi /caminho/Musashi --game-version 1.1 --original "Metal Gear (v1.1).md"` (ou `--game-version 1.2`). Monta um simulador 68000 mínimo (`tests/sim/mgsim.c`, VDP/SRAM/Z80 simplificados) e roda a original e a corrigida nos mesmos cenários: perda e restauração de itens/rank no save→reboot→load, slots independentes, Z80 que nunca responde (a original trava, a corrigida segue), paridade com Z80 normal, ausência de acessos ímpares/RAM não inicializada, regras de hardware real e a assinatura `MGSV`. Resultado: v1.2 10/10, v1.1 11/11. Também: 12 sementes de RAM inicial aleatória levam a v1.1 corrigida ao jogo sem violações.

Verificado também (sob xvfb): BlastEm roda a v1.1 corrigida até a jogabilidade e a tela do transceiver; na v1.2, BlastEm (U, J, E/PAL), RetroArch com o core BlastEm e mednafen 1.29 chegam ao jogo, e itens (Ration, Card 1, binóculo) foram gravados no `.srm` e restaurados pelo LOAD. O build para macOS e Windows está no CI (`.github/workflows/ci.yml`) e **não foi executado** nesta máquina.

## Compatibilidade com hardware real (auditoria estática e simulada)

Vale para a v1.2 corrigida (a original passa nas mesmas verificações); a v1.1 passou nas mesmas regras de bus/DMA/Z80 do simulador. Isto reduz risco, **não** substitui teste em console:

- Cabeçalho: `SEGA MEGA DRIVE`, regiões `JUE`, checksum válido, SRAM `RA F8 20` em `0x200001–0x20FFFF` (bytes ímpares, bateria). O mapper `A130F1` só liga a SRAM durante SAVE/LOAD.
- TMSS: o boot lê `0xA10001` e escreve `SEGA` em `0xA14000` quando há TMSS, antes de qualquer acesso ao VDP. No simulador esse ramo não é exercitado (ele devolve versão 0).
- Barramento do Z80: o simulador modela BUSREQ/BUSACK. Em 3000 quadros nenhum acesso do 68000 à RAM do Z80 ou ao YM2612 ocorre sem o barramento, nem com o Z80 em reset, e nenhum pulso de reset é curto demais.
- DMA ROM→VRAM: nenhuma cruza o limite de 128 KB. Sem acessos ímpares e sem leitura de RAM não inicializada.
- Não coberto: temporização de clones (FPGA/SoC), cadência de leitura do controle de 6 botões, PAL/50 Hz no aparelho, timing real do handshake com o Z80.

## Limitações

- **v1.2 testada em flashcart** (`.bin` roda como esperado). **v1.1 não foi testada em hardware**, só no simulador e em emuladores. Cartucho repro com bateria e consoles clone não foram testados. O travamento original não foi reproduzido em nenhum emulador; os timeouts do Z80 são a melhor hipótese, não uma prova.
- Salvar pelo menu de pause não foi automatizado; foi feito à mão no RetroArch (v1.2).
- Os laços em `0xA00160` não foram alterados (veja acima).
- Continuar depois de morrer ainda restaura itens do último checkpoint.
- Os instaladores nativos só podem ser gerados no próprio sistema (jpackage); o jar roda em qualquer um com Java 21+.

## Estrutura

```
docs/                  análise da v1.1 (v1.1-analise.md, v1.1-analysis.md)
app/                   programa Kotlin Multiplatform (commonMain: motor, estado, tela; desktopMain: janela e arquivos)
  .../core/            RomPatcher, RomProfiles (tabelas de patch), PatchBlobs (gerado), Sha256
patches/               rotinas 68000 (.s), binários montados (.bin/.sym) e build_patches.sh
tests/run_tests.py     testes de comportamento da ROM;  tests/sim/mgsim.c  simulador mínimo (Musashi)
.github/workflows/     CI (Linux, macOS, Windows)
```

## Apoie o projeto

Se este patch foi útil, você pode me apoiar em [buymeacoffee.com/legendaryredfox](https://buymeacoffee.com/legendaryredfox).

---

<a id="русский"></a>

# Русский

Программа с графическим интерфейсом (и режимом командной строки), которая применяет исправления для **реального железа** (репро-картриджи с батарейкой и флеш-картриджи) к порту **Metal Gear** для Mega Drive/Genesis
(H0ffman, Ice Dragon Dee, Inglebard, Toni Galvez и 5customs, по игре Konami). Написана на **Kotlin Multiplatform** (Compose Desktop): работает в **Linux, macOS и Windows**.

> В репозитории **нет ROM**. Программа исправляет *вашу* копию. Неофициальный проект, не связан с Konami и авторами порта.

| Версия | Обычное имя файла | Размер | SHA-256 |
|--------|-------------------|--------|---------|
| v1.2 | `Metal Gear (World) (v1.2) (Aftermarket) (Unl).gen` | 2 МБ | `1707f5534028cd4a3d049a552b1567fc7a3100598b450a976ed0690754be8f6a` |
| v1.1 | `Metal Gear (v1.1).md` | 1,5 МБ | `8073e3f1e966d8f6ab5b97f4d7e96208dd32dbea01997c3a4b5e2c5195b06f8b` |

## Как использовать

**Графический интерфейс:** `java -jar MetalGearPatcher-<ОС>-<арх>-<версия>.jar` (Java 21+) или нативный установщик (`.deb`, `.dmg`, `.msi`). Выберите ROM (или перетащите файл в окно), отметьте исправления и нажмите *Применить исправления*.
Программа определяет версию по SHA-256, проверяет исходные байты каждого патча, а после сборки проверяет, что каждый изменённый байт принадлежит патчу и контрольная сумма заголовка верна. Записываются `... [fixed].gen` и `... [fixed].bin` (одинаковое содержимое; берите расширение, которое показывает меню вашего флеш-картриджа). Язык интерфейса (португальский, английский, русский и китайский) берётся из системы и переключается в окне. Кнопка *Справка* показывает общие инструкции, требования (библиотеки и программы) и список файлов.

**Командная строка:** `java -jar MetalGearPatcher-*.jar --cli ROM [-o ВЫХОД] [--only save,z80,ram] [--force] [--check]`. Код возврата: 0 = успех, 1 = ошибка, 2 = неверное использование. `--force` принимает изменённый дамп (другой хеш), если все проверяемые байты совпадают.

## Что меняется

**v1.2** (изменено 336 байт):

| # | Где | Что |
|---|-----|-----|
| 1 | 0x1476 → 0x1BD880 | **SAVE** теперь записывает **текущие** звание, патроны, снаряжение и оружие. Раньше записывался только снимок последнего чекпоинта, поэтому предметы/звание, полученные после него (или с начала игры), терялись |
| 2 | 12 мест + 0xFD14 + 0x23980 → 0x1BD900 | Ожидания Z80 (BUSACK по `0xA11100` и флаг готовности по `0xA00102`) получили **тайм-аут**; раньше были бесконечными и могли зависнуть на чёрном экране |
| 3 | 0x049C | Очистка RAM при загрузке охватывает `0xFF0000–0xFFFFEF` (раньше только до `0xFF7C53`) |
| 4 | заголовок 0x18E / 0x1A4 | Контрольная сумма пересчитана; ROM end = 0x1BD9FF |

**v1.1** (изменено 331 байт; код тот же, что в v1.2, но со сдвигом и более старой звуковой библиотекой SGDK):

| # | Где | Что |
|---|-----|-----|
| 1 | 0x14AC → 0x13FE80 | То же исправление **SAVE** (формат SRAM прежний, старые сохранения загружаются) |
| 2 | 8 ожиданий BUSACK (0xF9F8, 0xFA28, 0xFA7C, 0xFAFC, 0xFBA2, 0xFD38, 0xFDB6, 0xFE1E) + 5 ожиданий флага готовности (0xFF18, 0x10548, 0x10E80, 0x10F0C, 0x10FAC) → 0x13FF00 | **Тайм-ауты** Z80; `Z80_DEAD` лежит в `0xFF9000` |
| 3 | 0x04A0 | Очистка RAM при загрузке охватывает `0xFF0000–0xFFDFEF`. До самого верха, как в v1.2, **нельзя**: стек v1.1 начинается с `0xFFE000` и хранит адрес возврата процедуры очистки |
| 4 | 0x0486 и 0x1630 | **Убирает контрольную сумму ROM** (обязательно, см. ниже) |
| 5 | заголовок 0x18E / 0x1A4 | Контрольная сумма пересчитана; ROM end = 0x13FFFF |

### Технические детали

- **Сохранение:** игра держит снимок размером 664 байта (таблица `(смещение, размер)`, 6 диапазонов RAM), который обновляется только в комнатах-чекпоинтах. SAVE копировал этот снимок в слот SRAM (5 слотов по 664 байта с `0x200011`, нечётные байты, подпись `MGSV`).
  Новая процедура (`patches/sync_inventory.s`) сначала копирует: звание (a5+0x235E), патроны (a5+0x25AC, 80 Б), предметы+флаги (a5+0x277C, 144 Б) и оружие+флаги (a5+0x274C, 40 Б). Позиция/комната по-прежнему берутся из чекпоинта (`a5 = 0xFF026C`). В обеих версиях процедура одна и та же.
- **Z80:** циклы BUSACK ждут ~3,6 мс, циклы флага готовности ~0,5 с. По тайм-ауту выставляется `Z80_DEAD`, и следующие ожидания делают одно чтение; если Z80 снова отвечает, флаг сбрасывается. Игра продолжает работу без звука вместо зависания. При исправном Z80 поведение идентично оригиналу (`patches/z80_timeouts.s` и `patches/z80_timeouts_v11.s`). Тайм-ауты зависят от очистки RAM (флаг должен начинаться с нуля), поэтому программа включает их вместе.
- **Контрольная сумма ROM (только v1.1):** процедура в `0x93FC`/`0x798` суммирует ROM от `0x474` до `0xF96C` и **прибавляет разницу к подписи `MGSV`** блока сохранения. В целой ROM разница равна 0. Если изменится любой байт этого диапазона (а SAVE и очистка RAM лежат в нём), SAVE запишет неверную подпись, и LOAD будет игнорировать сохранение. В v1.2 эту проверку убрали; здесь оба вызова заменены на NOP. Симулятор воспроизводит эффект: один изменённый байт в исходной v1.1 даёт подпись `MGSU`.
- **RAM:** в v1.1 переменные звукового драйвера и уровня прерываний лежат в `0xFF7C5E` и `0xFF7D3A`, выше того, что очищала исходная загрузка (`0xFF7C53`); симулятор показывает чтение неинициализированной RAM там на 11-м кадре исходной ROM и ни одного в исправленной. В v1.2 драйвер читает `0xFF86AA` и `0xFF85D0`.

### Другие проверенные улучшения для v1.1

Сделано: перечисленное выше. Проверено и **не** сделано (подробности, адреса и свидетельства в [docs/v1.1-analysis.md](docs/v1.1-analysis.md), на английском):

- 19 чтений `0xA00160` (очередь команд Z80) без тайм-аута, в разных формах; в v1.2 их тоже оставили. Нужен отдельный патч для каждого места.
- Переменные SGDK (зерно `random()`, счётчик тиков, контекст многозадачности) лежат по адресам ROM (`0x13FE10–0x13FE5A`), запись в них ничего не делает. Использующий их код нигде не вызывается, поэтому он безвреден.
- Различия в логике между v1.1 и v1.2 вне звукового драйвера: меню из 4 пунктов становится меню из 5 (`0xF68`), а флаг `a5@(0)` получает значение 1 в конце процедуры (`0x6A54`). Назначение неясно; не переносилось.

## Сборка и тесты

Требуется JDK 21+ (остальное скачает Gradle). Для пересборки `patches/*.s`: `sudo apt install binutils-m68k-linux-gnu`.

```
./gradlew :app:run                          открыть интерфейс
./gradlew :app:desktopTest                  модульные тесты и тесты интерфейса
MG_ROM_DIR=/папка/с/rom ./gradlew :app:desktopTest   включая тесты с настоящими ROM (без них пропускаются)
./gradlew :app:packageUberJarForCurrentOS   один jar для текущей системы
./gradlew :app:packageDistributionForCurrentOS   нативный установщик (.deb/.dmg/.msi) текущей системы
./patches/build_patches.sh                  пересобрать процедуры 68000 и сгенерировать PatchBlobs.kt
```

**Тесты на Kotlin** (65): SHA-256 (векторы и сверка с JDK), таблицы патчей (адреса, пересечения, цели jsr/jmp), движок (определение, сборка, подмножества, отказы), состояние экрана, CLI, тексты на четырёх языках и интерфейс на Compose (выбрать ROM, применить, сменить язык, справка). С настоящими ROM: результат для v1.2 **байт в байт совпадает** с ROM, уже проверенным на флеш-картридже (SHA-256 `0c52b1e2…c646`, 336 байт), а v1.1 даёт SHA-256 `be4ea50d…07e5`. Тесты на синтетическом ROM не заметят неверный адрес в таблице (он собирается из самой таблицы); это замечают только тесты с настоящим ROM.

**Поведение ROM** (`tests/run_tests.py`, нужны `gcc` и клон [Musashi](https://github.com/kstenerud/Musashi)):
`python3 tests/run_tests.py --musashi /путь/Musashi --game-version 1.1 --original "Metal Gear (v1.1).md"` (или `--game-version 1.2`). Собирается минимальный симулятор 68000 (`tests/sim/mgsim.c`, упрощённые VDP/SRAM/Z80), и оригинальный и исправленный ROM прогоняются по одним сценариям: потеря и восстановление предметов/звания при сохранении→перезагрузке→загрузке, независимые слоты, Z80, который никогда не отвечает (оригинал виснет, исправленный продолжает), совпадение при исправном Z80, отсутствие нечётных обращений/неинициализированной RAM, правила реального железа и подпись `MGSV`. Результат: v1.2 10/10, v1.1 11/11. Также: 12 случайных начальных состояний RAM доводят исправленную v1.1 до игры без нарушений.

Также проверено (под xvfb): BlastEm запускает исправленную v1.1 до геймплея и экрана рации; для v1.2 BlastEm (U, J, E/PAL), RetroArch с ядром BlastEm и mednafen 1.29 доходят до игры, а предметы (Ration, Card 1, бинокль) записались в `.srm` и восстановились через LOAD. Сборка для macOS и Windows описана в CI (`.github/workflows/ci.yml`) и **на этой машине не запускалась**.

## Совместимость с реальным железом (статический и симулированный аудит)

Относится к исправленной v1.2 (оригинал проходит те же проверки); v1.1 прошла те же правила шины/DMA/Z80 в симуляторе. Это снижает риск, но **не заменяет** тест на консоли:

- Заголовок: `SEGA MEGA DRIVE`, регионы `JUE`, корректная контрольная сумма, SRAM `RA F8 20` в `0x200001–0x20FFFF` (нечётные байты, батарея). Маппер `A130F1` включает SRAM только на время SAVE/LOAD.
- TMSS: при загрузке читается `0xA10001`, и при наличии TMSS в `0xA14000` пишется `SEGA` до любого обращения к VDP. В симуляторе эта ветка не исполняется (он возвращает версию 0).
- Шина Z80: симулятор моделирует BUSREQ/BUSACK. За 3000 кадров нет обращений 68000 к ОЗУ Z80 или YM2612 без шины или при сбросе Z80, и нет слишком коротких импульсов сброса.
- DMA ROM→VRAM: ни один не пересекает границу 128 КБ. Нет нечётных обращений и чтения неинициализированной RAM.
- Не покрыто: тайминги клонов (FPGA/SoC), опрос 6-кнопочного геймпада, PAL/50 Гц на устройстве, реальные тайминги рукопожатия с Z80.

## Ограничения

- **v1.2 проверена на флеш-картридже** (`.bin` работает как ожидалось). **v1.1 на железе не проверялась**, только в симуляторе и эмуляторах. Репро-картридж с батарейкой и клоны консоли не тестировались. Исходное зависание не воспроизведено ни в одном эмуляторе; тайм-ауты Z80 - лучшая гипотеза, а не доказательство.
- Сохранение через меню паузы не автоматизировано; выполнено вручную в RetroArch (v1.2).
- Циклы по `0xA00160` не изменены (см. выше).
- Продолжение после смерти по-прежнему восстанавливает предметы с последнего чекпоинта.
- Нативные установщики собираются только в своей системе (jpackage); jar работает везде, где есть Java 21+.

## Структура

```
app/                   программа Kotlin Multiplatform (commonMain: движок, состояние, экран; desktopMain: окно и файлы)
  .../core/            RomPatcher, RomProfiles (таблицы патчей), PatchBlobs (генерируется), Sha256
patches/               процедуры 68000 (.s), собранные файлы (.bin/.sym) и build_patches.sh
tests/run_tests.py     тесты поведения ROM;  tests/sim/mgsim.c  минимальный симулятор (Musashi)
.github/workflows/     CI (Linux, macOS, Windows)
```

## Поддержать проект

Если патч оказался полезен, можно поддержать меня на [buymeacoffee.com/legendaryredfox](https://buymeacoffee.com/legendaryredfox).

---

<a id="简体中文"></a>

# 简体中文

这是一个带图形界面（也有命令行模式）的程序，用于为 Mega Drive/Genesis 版 **Metal Gear** 移植版（H0ffman、Ice Dragon Dee、Inglebard、Toni Galvez 和 5customs，基于 Konami 的游戏）应用**真机**修复（带电池的复刻卡带和烧录卡）。使用 **Kotlin Multiplatform**（Compose Desktop）编写，可在 **Linux、macOS 和 Windows** 上运行。

> 本仓库**不包含 ROM**。程序修复的是*您自己*的副本。非官方项目，与 Konami 及移植版作者无关。

| 版本 | 常见文件名 | 大小 | SHA-256 |
|------|-----------|------|---------|
| v1.2 | `Metal Gear (World) (v1.2) (Aftermarket) (Unl).gen` | 2 MB | `1707f5534028cd4a3d049a552b1567fc7a3100598b450a976ed0690754be8f6a` |
| v1.1 | `Metal Gear (v1.1).md` | 1.5 MB | `8073e3f1e966d8f6ab5b97f4d7e96208dd32dbea01997c3a4b5e2c5195b06f8b` |

## 使用方法

**图形界面：** `java -jar MetalGearPatcher-<系统>-<架构>-<版本>.jar`（需要 Java 21+），或使用本机安装包（`.deb`、`.dmg`、`.msi`）。选择 ROM（或把文件拖到窗口上），勾选修复项，然后点击“应用修复”。
程序通过 SHA-256 识别版本，检查每个补丁的原始字节，构建完成后验证每个被改动的字节都属于某个补丁且头部校验和正确。它会写出 `... [fixed].gen` 和 `... [fixed].bin`（内容相同，请使用您的烧录卡菜单所列的扩展名）。界面语言（葡萄牙语、英语、俄语、中文）跟随系统，也可在窗口中切换。“帮助”按钮会显示通用说明、运行要求（库和程序）以及文件列表。

**命令行：** `java -jar MetalGearPatcher-*.jar --cli ROM [-o 输出] [--only save,z80,ram] [--force] [--check]`。退出码 0 = 成功，1 = 错误，2 = 用法错误。`--force` 在所有被检查的字节仍然匹配时接受已修改的转储（哈希不同）。

## 修改内容

**v1.2**（修改 336 字节）：

| # | 位置 | 内容 |
|---|------|------|
| 1 | 0x1476 → 0x1BD880 | **SAVE** 现在写入**当前**的军衔、弹药、装备和武器。以前只写入最后一个检查点的快照，因此之后（或从开局起）获得的物品/军衔会丢失 |
| 2 | 12 处 + 0xFD14 + 0x23980 → 0x1BD900 | 对 Z80 的等待（`0xA11100` 上的 BUSACK 和 `0xA00102` 上的就绪标志）加入**超时**；以前是无限等待，可能让游戏卡在黑屏 |
| 3 | 0x049C | 启动时的 RAM 清除覆盖 `0xFF0000–0xFFFFEF`（以前只到 `0xFF7C53`） |
| 4 | 头部 0x18E / 0x1A4 | 重新计算校验和；ROM end = 0x1BD9FF |

**v1.1**（修改 331 字节；代码与 v1.2 相同但有偏移，声音库是较旧的 SGDK）：

| # | 位置 | 内容 |
|---|------|------|
| 1 | 0x14AC → 0x13FE80 | 同样的 **SAVE** 修复（SRAM 格式不变，旧存档仍可读取） |
| 2 | 8 处 BUSACK 等待（0xF9F8、0xFA28、0xFA7C、0xFAFC、0xFBA2、0xFD38、0xFDB6、0xFE1E）+ 5 处就绪标志等待（0xFF18、0x10548、0x10E80、0x10F0C、0x10FAC）→ 0x13FF00 | Z80 **超时**；`Z80_DEAD` 位于 `0xFF9000` |
| 3 | 0x04A0 | 启动时的 RAM 清除覆盖 `0xFF0000–0xFFDFEF`。**不能**像 v1.2 那样清到顶部：v1.1 的堆栈从 `0xFFE000` 开始，并保存着清除例程的返回地址 |
| 4 | 0x0486 和 0x1630 | **移除 ROM 校验和**（必需，见下文） |
| 5 | 头部 0x18E / 0x1A4 | 重新计算校验和；ROM end = 0x13FFFF |

### 技术细节

- **存档：** 游戏保存一份 664 字节的快照（`(偏移, 大小)` 表，6 段 RAM），只在检查点房间更新。SAVE 以前把这份快照复制到 SRAM 槽位（5 个 664 字节的槽位，从 `0x200011` 开始，奇数字节，签名 `MGSV`）。
  新例程（`patches/sync_inventory.s`）先复制：军衔（a5+0x235E）、弹药（a5+0x25AC，80 B）、物品+标志（a5+0x277C，144 B）、武器+标志（a5+0x274C，40 B）。位置/房间仍取自检查点（`a5 = 0xFF026C`）。两个版本使用同一个例程。
- **Z80：** BUSACK 循环等待约 3.6 ms，就绪标志循环约 0.5 s。超时后置位 `Z80_DEAD`，之后的等待只读取一次；如果 Z80 再次响应则清除该标志。游戏不再卡死，而是无声继续。Z80 正常时行为与原版一致（`patches/z80_timeouts.s` 和 `patches/z80_timeouts_v11.s`）。超时依赖 RAM 清除（该标志必须从 0 开始），因此程序会同时启用两者。
- **ROM 校验和（仅 v1.1）：** `0x93FC`/`0x798` 处的例程对 `0x474` 到 `0xF96C` 的 ROM 求和，并**把差值加到存档块的 `MGSV` 签名上**。ROM 完好时差值为 0。如果该范围内任何字节改变（SAVE 钩子和 RAM 清除都在其中），SAVE 会写入无效签名，LOAD 就会忽略存档。v1.2 移除了这项检查；这里把两处调用改成 NOP。模拟器重现了这一效果：原版 v1.1 改动一个字节后签名变为 `MGSU`。
- **RAM：** 在 v1.1 中，声音驱动和中断级别变量位于 `0xFF7C5E` 和 `0xFF7D3A`，高于原版启动时清除的范围（`0xFF7C53`）；模拟器显示原版 ROM 在第 11 帧读取了那里的未初始化 RAM，而修复后的 ROM 没有。在 v1.2 中驱动读取的是 `0xFF86AA` 和 `0xFF85D0`。

### 在 v1.1 上检查过的其他改进

已完成：上文所列。已检查但**未做**（详细地址与证据见 [docs/v1.1-analysis.md](docs/v1.1-analysis.md)，英文）：

- 19 处对 `0xA00160`（Z80 命令队列）的读取没有超时，形式各不相同；v1.2 也没有处理。每处都需要单独的补丁。
- SGDK 变量（`random()` 种子、节拍计数器、多任务上下文）位于 ROM 地址（`0x13FE10–0x13FE5A`），写入它们没有任何效果。使用它们的代码从未被调用，因此无害。
- v1.1 与 v1.2 在声音驱动之外的逻辑差异：一个 4 项菜单变成 5 项（`0xF68`），以及 `a5@(0)` 处的标志在某个例程结束时被置为 1（`0x6A54`）。意图不明；未移植。

## 构建与测试

需要 JDK 21+（其余由 Gradle 下载）。重新汇编 `patches/*.s`：`sudo apt install binutils-m68k-linux-gnu`。

```
./gradlew :app:run                          打开界面
./gradlew :app:desktopTest                  单元测试和界面测试
MG_ROM_DIR=/存放rom的文件夹 ./gradlew :app:desktopTest   同时运行真实 ROM 测试（没有 ROM 时跳过）
./gradlew :app:packageUberJarForCurrentOS   生成当前系统的单个 jar
./gradlew :app:packageDistributionForCurrentOS   生成当前系统的本机安装包（.deb/.dmg/.msi）
./patches/build_patches.sh                  重新汇编 68000 例程并重新生成 PatchBlobs.kt
```

**Kotlin 测试**（65 个）：SHA-256（标准向量以及与 JDK 的对照）、补丁表（地址、重叠、jsr/jmp 目标）、引擎（识别、构建、子集、拒绝）、界面状态、命令行、四种语言的文字，以及 Compose 界面（选择 ROM、应用、切换语言、帮助）。使用真实 ROM 时：v1.2 的输出与已在烧录卡上测试过的 ROM **逐字节相同**（SHA-256 `0c52b1e2…c646`，336 字节），v1.1 构建出的 SHA-256 为 `be4ea50d…07e5`。基于合成 ROM 的测试无法发现补丁表中的错误地址（ROM 本身由该表构建）；只有真实 ROM 测试能发现。

**ROM 行为**（`tests/run_tests.py`，需要 `gcc` 和 [Musashi](https://github.com/kstenerud/Musashi) 源码）：
`python3 tests/run_tests.py --musashi /路径/Musashi --game-version 1.1 --original "Metal Gear (v1.1).md"`（或 `--game-version 1.2`）。它构建一个最小的 68000 模拟器（`tests/sim/mgsim.c`，简化的 VDP/SRAM/Z80），让原版和修复版 ROM 运行相同的场景：存档→重启→读档时物品/军衔的丢失与恢复、槽位相互独立、永不响应的 Z80（原版卡死，修复版继续）、Z80 正常时与原版一致、没有奇地址访问或未初始化 RAM、真机规则以及 `MGSV` 签名。结果：v1.2 为 10/10，v1.1 为 11/11。另外：12 个随机初始 RAM 种子都能让修复后的 v1.1 无违规地进入游戏。

另外检查过（在 xvfb 下）：BlastEm 能把修复后的 v1.1 运行到游戏画面和无线电画面；对于 v1.2，BlastEm（U、J、E/PAL）、带 BlastEm 核心的 RetroArch 和 mednafen 1.29 都能进入游戏，物品（Ration、Card 1、望远镜）被写入 `.srm` 并由 LOAD 恢复。macOS 和 Windows 的构建定义在 CI 中（`.github/workflows/ci.yml`），在本机上**没有运行过**。

## 真机兼容性（静态与模拟审计）

适用于修复后的 v1.2（原版通过同样的检查）；v1.1 在模拟器中通过了同样的总线/DMA/Z80 规则。这能降低风险，但**不能替代**主机上的测试：

- 头部：`SEGA MEGA DRIVE`，地区 `JUE`，校验和有效，SRAM `RA F8 20` 位于 `0x200001–0x20FFFF`（奇数字节，带电池）。`A130F1` 映射器只在 SAVE/LOAD 期间启用 SRAM。
- TMSS：启动时读取 `0xA10001`，存在 TMSS 时在访问 VDP 之前向 `0xA14000` 写入 `SEGA`。模拟器不会执行该分支（它返回版本 0）。
- Z80 总线：模拟器模拟 BUSREQ/BUSACK。在 3000 帧内，没有 68000 在未持有总线或 Z80 处于复位时访问 Z80 RAM 或 YM2612，也没有过短的复位脉冲。
- ROM→VRAM DMA：没有跨越 128 KB 边界的。没有奇地址访问，也没有读取未初始化 RAM。
- 未覆盖：克隆机的时序（FPGA/SoC）、六键手柄读取节奏、设备上的 PAL/50 Hz、与 Z80 握手的真实时序。

## 局限

- **v1.2 已在烧录卡上测试**（`.bin` 按预期运行）。**v1.1 未在真机上测试**，只在模拟器和仿真器中验证过。带电池的复刻卡带和克隆主机未测试。原版的卡死在任何仿真器中都没有复现；Z80 超时是最佳假设，而非证明。
- 通过暂停菜单存档没有自动化；v1.2 是在 RetroArch 中手动完成的。
- `0xA00160` 上的循环没有改动（见上文）。
- 死亡后继续游戏仍会从最后一个检查点恢复物品。
- 本机安装包只能在各自的系统上构建（jpackage）；jar 在任何有 Java 21+ 的系统上都能运行。

## 目录结构

```
docs/                  v1.1 分析（v1.1-analise.md、v1.1-analysis.md）
app/                   Kotlin Multiplatform 程序（commonMain：引擎、状态、界面；desktopMain：窗口和文件）
  .../core/            RomPatcher、RomProfiles（补丁表）、PatchBlobs（自动生成）、Sha256
patches/               68000 例程（.s）、汇编结果（.bin/.sym）和 build_patches.sh
tests/run_tests.py     ROM 行为测试；  tests/sim/mgsim.c  最小模拟器（Musashi）
.github/workflows/     CI（Linux、macOS、Windows）
```

## 支持项目

如果这个补丁对您有帮助，可以在 [buymeacoffee.com/legendaryredfox](https://buymeacoffee.com/legendaryredfox) 支持我。

---

<a id="english"></a>

# English

A program with a graphical interface (and a command-line mode) that applies **real-hardware** fixes (battery-backed repro carts and flashcarts) to the **Metal Gear** port for Mega Drive/Genesis
(H0ffman, Ice Dragon Dee, Inglebard, Toni Galvez and 5customs, based on Konami's game). Written in **Kotlin Multiplatform** (Compose Desktop): runs on **Linux, macOS and Windows**.

> This repository **contains no ROMs**. The program fixes *your own* copy. Unofficial project, not affiliated with Konami or the port's authors.

| Version | Usual file name | Size | SHA-256 |
|---------|-----------------|------|---------|
| v1.2 | `Metal Gear (World) (v1.2) (Aftermarket) (Unl).gen` | 2 MB | `1707f5534028cd4a3d049a552b1567fc7a3100598b450a976ed0690754be8f6a` |
| v1.1 | `Metal Gear (v1.1).md` | 1.5 MB | `8073e3f1e966d8f6ab5b97f4d7e96208dd32dbea01997c3a4b5e2c5195b06f8b` |

## Usage

**GUI:** `java -jar MetalGearPatcher-<os>-<arch>-<version>.jar` (Java 21+), or the native installer (`.deb`, `.dmg`, `.msi`). Choose the ROM (or drop the file on the window), tick the fixes and press *Apply fixes*.
The program recognizes the version by SHA-256, checks the original bytes of every patch and, after building, verifies that every changed byte belongs to a patch and that the header checksum is right. It writes `... [fixed].gen` and `... [fixed].bin` (identical contents; use the extension your flashcart menu lists). The interface language (Portuguese, English, Russian and Chinese) follows the system and can be changed in the window. The *Help* button shows general instructions, the requirements (libraries and programs) and the list of files.

**Command line:** `java -jar MetalGearPatcher-*.jar --cli ROM [-o OUTPUT] [--only save,z80,ram] [--force] [--check]`. Exit status 0 = ok, 1 = error, 2 = bad usage. `--force` accepts a modified dump (different hash) when every checked byte still matches.

## What changes

**v1.2** (336 bytes changed):

| # | Where | What |
|---|-------|------|
| 1 | 0x1476 → 0x1BD880 | **SAVE** now writes the **current** rank, ammo, equipment and weapons. It used to write only the last checkpoint's snapshot, so items/rank obtained after it (or since the start) were lost |
| 2 | 12 sites + 0xFD14 + 0x23980 → 0x1BD900 | Z80 waits (BUSACK on `0xA11100` and the ready flag on `0xA00102`) get a **timeout**; they used to be infinite and could hang the game on a black screen |
| 3 | 0x049C | Boot RAM clear covers `0xFF0000–0xFFFFEF` (was only up to `0xFF7C53`) |
| 4 | header 0x18E / 0x1A4 | Checksum recalculated; ROM end = 0x1BD9FF |

**v1.1** (331 bytes changed; same code as v1.2, shifted, with an older SGDK sound library):

| # | Where | What |
|---|-------|------|
| 1 | 0x14AC → 0x13FE80 | The same **SAVE** fix (same SRAM format, old saves still load) |
| 2 | 8 BUSACK waits (0xF9F8, 0xFA28, 0xFA7C, 0xFAFC, 0xFBA2, 0xFD38, 0xFDB6, 0xFE1E) + 5 ready-flag waits (0xFF18, 0x10548, 0x10E80, 0x10F0C, 0x10FAC) → 0x13FF00 | Z80 **timeouts**; `Z80_DEAD` lives at `0xFF9000` |
| 3 | 0x04A0 | Boot RAM clear covers `0xFF0000–0xFFDFEF`. It must **not** go to the top as in v1.2: the v1.1 stack starts at `0xFFE000` and holds the return address of the clear routine |
| 4 | 0x0486 and 0x1630 | **Removes the ROM checksum** (required, see below) |
| 5 | header 0x18E / 0x1A4 | Checksum recalculated; ROM end = 0x13FFFF |

### Technical details

- **Save:** the game keeps a 664-byte snapshot (`(offset, size)` table, 6 RAM ranges) refreshed only in checkpoint rooms. SAVE copied that snapshot into the SRAM slot (5 slots of 664 bytes at `0x200011`, odd bytes, signature `MGSV`).
  The new routine (`patches/sync_inventory.s`) first copies: rank (a5+0x235E), ammo (a5+0x25AC, 80 B), items+flags (a5+0x277C, 144 B) and weapons+flags (a5+0x274C, 40 B). Position/room still come from the checkpoint (`a5 = 0xFF026C`). It is the same routine in both versions.
- **Z80:** the BUSACK loops wait ~3.6 ms; the ready-flag loops ~0.5 s. On timeout `Z80_DEAD` is set and later waits only look once; if the Z80 answers again the flag is cleared. The game carries on without sound instead of hanging. With a healthy Z80 behaviour is identical to the original (`patches/z80_timeouts.s` and `patches/z80_timeouts_v11.s`). The timeouts depend on the RAM clear (the flag must start at zero), so the program enables both together.
- **ROM checksum (v1.1 only):** the routine at `0x93FC`/`0x798` sums the ROM from `0x474` to `0xF96C` and **adds the difference to the `MGSV` signature** of the save block. On an intact ROM the difference is 0. If any byte in that range changes (and both the SAVE hook and the RAM clear are in it), SAVE writes a bad signature and LOAD ignores the save. v1.2 removed the check; here both calls become NOPs. The simulator reproduces the effect: one changed byte in the original v1.1 gives the signature `MGSU`.
- **RAM:** in v1.1 the sound driver and interrupt-level variables sit at `0xFF7C5E` and `0xFF7D3A`, above what the original boot cleared (`0xFF7C53`); the simulator shows uninitialised RAM reads there at frame 11 of the original ROM and none in the fixed one. In v1.2 the driver reads `0xFF86AA` and `0xFF85D0`.

### Other improvements checked on v1.1

Done: those listed above. Checked and **not** done (details, addresses and evidence in [docs/v1.1-analysis.md](docs/v1.1-analysis.md)):

- 19 reads of `0xA00160` (the Z80 command queue) without a timeout, in different shapes; v1.2 left them too. Each site needs its own patch.
- SGDK variables (the `random()` seed, a tick counter, the multitasking context) sit at ROM addresses (`0x13FE10–0x13FE5A`), so writing to them does nothing. The code that uses them is never called, so it is harmless.
- Logic differences between v1.1 and v1.2 outside the sound driver: a 4-entry menu becomes 5 (`0xF68`), and a flag at `a5@(0)` is set to 1 at the end of a routine (`0x6A54`). The intent is unclear; not ported.

## Build and test

Needs JDK 21+ (Gradle downloads the rest). To re-assemble `patches/*.s`: `sudo apt install binutils-m68k-linux-gnu`.

```
./gradlew :app:run                          open the interface
./gradlew :app:desktopTest                  unit and UI tests
MG_ROM_DIR=/folder/with/roms ./gradlew :app:desktopTest   also runs the real-ROM tests (skipped without them)
./gradlew :app:packageUberJarForCurrentOS   one jar for the current system
./gradlew :app:packageDistributionForCurrentOS   native installer (.deb/.dmg/.msi) of the current system
./patches/build_patches.sh                  re-assemble the 68000 routines and regenerate PatchBlobs.kt
```

**Kotlin tests** (65): SHA-256 (vectors and a cross-check against the JDK), patch tables (addresses, overlaps, jsr/jmp targets), the engine (identification, build, subsets, refusals), screen state, CLI, texts in the four languages and the Compose UI (choose ROM, apply, switch language, help). With the real ROMs: the v1.2 output is **byte-identical** to the ROM already tested on a flashcart (SHA-256 `0c52b1e2…c646`, 336 bytes), and v1.1 builds to SHA-256 `be4ea50d…07e5`. Tests on synthetic ROMs cannot catch a wrong address in the table (the ROM is built from the table itself); only the real-ROM tests can.

**ROM behaviour** (`tests/run_tests.py`, needs `gcc` and a [Musashi](https://github.com/kstenerud/Musashi) checkout):
`python3 tests/run_tests.py --musashi /path/to/Musashi --game-version 1.1 --original "Metal Gear (v1.1).md"` (or `--game-version 1.2`). It builds a minimal 68000 simulator (`tests/sim/mgsim.c`, simplified VDP/SRAM/Z80) and runs the original and the fixed ROM through the same scenarios: loss and restoration of items/rank on save→reboot→load, independent slots, a Z80 that never answers (the original hangs, the fixed one carries on), parity with a healthy Z80, no odd-address access or uninitialised RAM, real-hardware rules and the `MGSV` signature. Result: v1.2 10/10, v1.1 11/11. Also: 12 random initial-RAM seeds take the fixed v1.1 into the game with no violation.

Also checked (under xvfb): BlastEm runs the fixed v1.1 to gameplay and the transceiver screen; for v1.2, BlastEm (U, J, E/PAL), RetroArch with the BlastEm core and mednafen 1.29 reach the game, and items (Ration, Card 1, binoculars) were written to the `.srm` and restored by LOAD. The macOS and Windows builds are defined in CI (`.github/workflows/ci.yml`) and were **not run** on this machine.

## Real-hardware compatibility (static and simulated audit)

Applies to the fixed v1.2 (the original passes the same checks); v1.1 passed the same bus/DMA/Z80 rules in the simulator. This lowers risk, it does **not** replace a console test:

- Header: `SEGA MEGA DRIVE`, regions `JUE`, valid checksum, SRAM `RA F8 20` at `0x200001–0x20FFFF` (odd bytes, battery). The `A130F1` mapper only enables SRAM during SAVE/LOAD.
- TMSS: boot reads `0xA10001` and writes `SEGA` to `0xA14000` when a TMSS is present, before any VDP access. The simulator does not exercise that branch (it returns version 0).
- Z80 bus: the simulator models BUSREQ/BUSACK. In 3000 frames no 68000 access to Z80 RAM or the YM2612 happens without the bus or while the Z80 is in reset, and no reset pulse is too short.
- ROM→VRAM DMA: none crosses a 128 KB boundary. No odd accesses and no reads of uninitialised RAM.
- Not covered: clone timing (FPGA/SoC), 6-button pad read cadence, PAL/50 Hz on a device, real timing of the Z80 handshake.

## Limitations

- **v1.2 tested on a flashcart** (`.bin` runs as expected). **v1.1 was not tested on hardware**, only in the simulator and emulators. A battery-backed repro cart and clone consoles were not tested. The original hang was not reproduced in any emulator; the Z80 timeouts are the best hypothesis, not proof.
- Saving from the pause menu was not automated; it was done by hand in RetroArch (v1.2).
- The loops on `0xA00160` were not changed (see above).
- Continuing after death still restores items from the last checkpoint.
- Native installers can only be built on their own system (jpackage); the jar runs anywhere with Java 21+.

## Layout

```
docs/                  v1.1 analysis (v1.1-analise.md, v1.1-analysis.md)
app/                   Kotlin Multiplatform program (commonMain: engine, state, screen; desktopMain: window and files)
  .../core/            RomPatcher, RomProfiles (patch tables), PatchBlobs (generated), Sha256
patches/               68000 routines (.s), assembled files (.bin/.sym) and build_patches.sh
tests/run_tests.py     ROM behaviour tests;  tests/sim/mgsim.c  minimal simulator (Musashi)
.github/workflows/     CI (Linux, macOS, Windows)
```

## Support

If this patch was useful you can support me at [buymeacoffee.com/legendaryredfox](https://buymeacoffee.com/legendaryredfox).
