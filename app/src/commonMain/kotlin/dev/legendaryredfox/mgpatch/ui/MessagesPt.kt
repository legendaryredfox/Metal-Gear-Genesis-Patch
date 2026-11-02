package dev.legendaryredfox.mgpatch.ui

import dev.legendaryredfox.mgpatch.core.Identification.Reason
import dev.legendaryredfox.mgpatch.core.Identification.Reason.NOT_MEGA_DRIVE
import dev.legendaryredfox.mgpatch.core.Identification.Reason.SMD_FORMAT
import dev.legendaryredfox.mgpatch.core.Identification.Reason.TOO_SMALL
import dev.legendaryredfox.mgpatch.core.Identification.Reason.UNKNOWN_DUMP
import dev.legendaryredfox.mgpatch.core.PatchException.Kind
import dev.legendaryredfox.mgpatch.core.PatchException.Kind.AREA_TOO_SMALL
import dev.legendaryredfox.mgpatch.core.PatchException.Kind.BAD_CHECKSUM
import dev.legendaryredfox.mgpatch.core.PatchException.Kind.BYTES_MISMATCH
import dev.legendaryredfox.mgpatch.core.PatchException.Kind.FREE_AREA_NOT_EMPTY
import dev.legendaryredfox.mgpatch.core.PatchException.Kind.UNDECLARED_CHANGE
import dev.legendaryredfox.mgpatch.core.PatchException.Kind.WRONG_SIZE
import dev.legendaryredfox.mgpatch.core.PatchGroup
import dev.legendaryredfox.mgpatch.core.PatchGroup.RAM_CLEAR
import dev.legendaryredfox.mgpatch.core.PatchGroup.ROM_CHECK_BYPASS
import dev.legendaryredfox.mgpatch.core.PatchGroup.SAVE_SYNC
import dev.legendaryredfox.mgpatch.core.PatchGroup.Z80_TIMEOUTS

object MessagesPt : Messages {
    override val appTitle = "Metal Gear Genesis Patcher"
    override val appSubtitle = "Correções para hardware real do port de Metal Gear para Mega Drive (v1.1 e v1.2)"
    override val language = "Idioma"
    override val stepRom = "1. Sua ROM"
    override val chooseRom = "Escolher ROM..."
    override val dropHint = "ou solte o arquivo aqui"
    override val noRomChosen = "Nenhuma ROM escolhida. Este programa não contém ROMs: use o seu próprio dump."
    override val fileLabel = "Arquivo"
    override val sizeLabel = "Tamanho"
    override val hashLabel = "SHA-256"
    override val usingProfile = { p: String -> "Conjunto de patches: $p" }
    override val statusKnown = { p: String -> "Reconhecida: $p" }
    override val statusCompatible = { p: String -> "Parece ser $p, mas o arquivo não é o dump conhecido (hash diferente)." }
    override val statusAlreadyPatched = "Esta ROM já contém as correções. Use o dump original."
    override val statusUnrecognized = { r: Reason ->
        when (r) {
            TOO_SMALL -> "O arquivo é pequeno demais para ser uma ROM de Mega Drive."
            SMD_FORMAT -> "Parece um dump .smd (entrelaçado). Converta para .bin/.gen/.md simples antes."
            NOT_MEGA_DRIVE -> "Não é uma ROM de Mega Drive (sem cabeçalho SEGA)."
            UNKNOWN_DUMP -> "Não é um dump suportado de Metal Gear. Suportados: v1.1 (.md) e v1.2 (.gen)."
        }
    }
    override val allowCompatible = "Tentar mesmo assim (cada patch ainda confere os bytes originais)"
    override val readError = { d: String -> "Não foi possível ler o arquivo: $d" }
    override val stepFixes = "2. Correções a aplicar"
    override val fixTitle = { g: PatchGroup ->
        when (g) {
            SAVE_SYNC -> "SAVE guarda os itens atuais"
            Z80_TIMEOUTS -> "Timeouts da CPU de som"
            RAM_CLEAR -> "Limpeza completa da RAM no boot"
            ROM_CHECK_BYPASS -> "Desligar o checksum da ROM"
        }
    }
    override val fixDescription = { g: PatchGroup, profileId: String ->
        when (g) {
            SAVE_SYNC ->
                "O SAVE grava rank, munição, equipamento e armas atuais. Antes só o snapshot do último checkpoint era gravado, então itens obtidos depois dele se perdiam. Saves antigos continuam carregando."
            Z80_TIMEOUTS ->
                "As esperas pela CPU de som Z80 desistem depois de um tempo em vez de travar para sempre na tela preta. Se o Z80 nunca responder, o jogo segue sem som."
            RAM_CLEAR ->
                if (profileId == "v1.1") "Limpa toda a RAM de trabalho abaixo da pilha no boot (o original deixa as variáveis do driver de som aleatórias em hardware real)."
                else "Limpa toda a RAM de trabalho no boot (o original limpava só os primeiros 31 KB; o driver de som lia valores aleatórios acima disso)."
            ROM_CHECK_BYPASS ->
                "A v1.1 confere um checksum do próprio código e corrompe em silêncio a assinatura do save se um byte mudar. Isto desliga essa checagem (a v1.2 também a removeu)."
        }
    }
    override val tagRequired = "obrigatória"
    override val tagNeededBy = { n: String -> "necessária para \"$n\"" }
    override val stepOutput = "3. Saída"
    override val outputFolder = "Pasta de saída"
    override val change = "Alterar..."
    override val willWrite = "Serão gravados dois arquivos idênticos (use a extensão que o menu do seu flashcart lista):"
    override val willOverwrite = "Já existe um arquivo com este nome; ele será substituído."
    override val apply = "Aplicar correções"
    override val successTitle = "Pronto"
    override val successDetail = { changed: Int, checksum: String -> "$changed bytes alterados, checksum do cabeçalho $checksum. O resultado foi verificado (toda alteração pertence a uma correção, checksum válido)." }
    override val writtenFiles = "Arquivos gravados"
    override val sha256Output = "SHA-256 da ROM corrigida"
    override val failureTitle = "Nada foi gravado"
    override val failure = { k: Kind, d: String ->
        when (k) {
            WRONG_SIZE -> "Tamanho de ROM inesperado ($d bytes)."
            BYTES_MISMATCH -> "O código original em $d não é o que o patch espera. Não é um dump intacto da versão suportada."
            FREE_AREA_NOT_EMPTY -> "A área livre em $d não está vazia; a ROM parece já modificada."
            AREA_TOO_SMALL -> "Uma rotina do patch não cabe na área livre (em $d)."
            UNDECLARED_CHANGE -> "Verificação interna falhou: o byte $d mudou fora das faixas do patch."
            BAD_CHECKSUM -> "Verificação interna falhou: o checksum do cabeçalho está errado."
        }
    }
    override val writeFailure = { d: String -> "Não foi possível gravar a saída: $d" }
    override val nothingSelected = "Selecione ao menos uma correção."
    override val footer = "Projeto não oficial, sem ligação com a Konami nem com os autores do port. Nenhuma ROM é incluída ou baixada."
    override val testedNote = "ROM v1.2 corrigida testada em flashcart; v1.1 verificada no simulador e em emuladores. Não testada em cartucho real com bateria."
    override val help = "Ajuda"
    override val helpTitle = "Ajuda"
    override val helpClose = "Fechar"
    override val helpSections = listOf(
        HelpSection(
            "Como usar",
            listOf(
                "Obtenha o seu próprio dump de Metal Gear para Mega Drive (v1.1 em .md ou v1.2 em .gen). Nenhuma ROM é incluída ou baixada.",
                "Clique em \"Escolher ROM...\" ou solte o arquivo na janela. O programa mostra a versão reconhecida e o SHA-256.",
                "Deixe as correções marcadas (recomendado). As obrigatórias não podem ser desmarcadas, e a limpeza de RAM fica ligada enquanto os timeouts da CPU de som dependem dela.",
                "Escolha a pasta de saída (por padrão, a da ROM) e clique em \"Aplicar correções\". São gravados dois arquivos idênticos, [fixed].gen e [fixed].bin. O seu arquivo original nunca é alterado.",
                "Copie o .bin ou o .gen para o seu flashcart, ou grave no seu cartucho repro. Saves feitos com a ROM original continuam carregando.",
                "Pelo terminal: java -jar MetalGearPatcher-*.jar --cli ROM [-o SAÍDA] [--only save,z80,ram] [--force] [--check]",
            ),
        ),
        HelpSection(
            "Requisitos",
            listOf(
                "Para executar o programa: Java 21 ou mais novo (o .jar), ou o instalador nativo, que já traz o seu próprio Java. Nada mais é preciso para corrigir uma ROM.",
                "Linux: uma sessão gráfica (X11 ou Wayland) com as bibliotecas AWT usuais (libxtst6, libxrender1, libfreetype6). Para a interface em chinês, uma fonte CJK como a Noto Sans CJK.",
                "Para compilar do código-fonte: JDK 21+ e o wrapper do Gradle incluído (ele baixa Kotlin 2.4.21, Compose Multiplatform 1.12.2, Material 3 1.9.0 e kotlinx-coroutines 1.11.0).",
                "Para remontar as rotinas 68000: binutils-m68k-linux-gnu (m68k-linux-gnu-as, ld, objcopy, nm) e xxd.",
                "Para os testes de comportamento da ROM: gcc, um checkout do Musashi e Python 3 (só a biblioteca padrão).",
                "Para os instaladores nativos: jpackage (parte do JDK), mais dpkg e fakeroot para .deb, WiX para .msi, ferramentas de linha de comando do Xcode para .dmg. Cada um só pode ser gerado no próprio sistema.",
            ),
        ),
        HelpSection(
            "Arquivos",
            listOf(
                "Entrada: Metal Gear (World) (v1.2) (Aftermarket) (Unl).gen (2 MB) ou Metal Gear (v1.1).md (1,5 MB).",
                "Saída: \"... [fixed].gen\" e \"... [fixed].bin\" com o mesmo conteúdo, nomeados conforme a versão.",
                "app/: o programa Kotlin Multiplatform (core = motor e tabelas de patch, ui = tela e textos, desktopMain = janela e arquivos).",
                "patches/: sync_inventory.s, z80_timeouts.s e z80_timeouts_v11.s (fontes 68000), os respectivos .bin/.sym e build_patches.sh.",
                "tests/: run_tests.py e sim/mgsim.c (testes de comportamento da ROM). Os testes em Kotlin ficam em app/src/*Test/.",
                ".github/workflows/ci.yml: compila e testa em Linux, macOS e Windows.",
                "README.md: documentação completa em português, russo e inglês.",
            ),
        ),
    )
}
