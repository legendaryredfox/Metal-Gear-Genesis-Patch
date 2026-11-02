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

object MessagesZh : Messages {
    override val appTitle = "Metal Gear Genesis Patcher"
    override val appSubtitle = "Metal Gear Mega Drive 移植版（v1.1 和 v1.2）的真机修复工具"
    override val language = "语言"
    override val stepRom = "1. 您的 ROM"
    override val chooseRom = "选择 ROM..."
    override val dropHint = "或把文件拖到这里"
    override val noRomChosen = "尚未选择 ROM。本程序不包含任何 ROM，请使用您自己的转储文件。"
    override val fileLabel = "文件"
    override val sizeLabel = "大小"
    override val hashLabel = "SHA-256"
    override val usingProfile = { p: String -> "补丁集：$p" }
    override val statusKnown = { p: String -> "已识别：$p" }
    override val statusCompatible = { p: String -> "看起来是 $p，但文件不是已知转储（哈希不同）。" }
    override val statusAlreadyPatched = "此 ROM 已包含修复，请使用原始转储。"
    override val statusUnrecognized = { r: Reason ->
        when (r) {
            TOO_SMALL -> "文件太小，不可能是 Mega Drive ROM。"
            SMD_FORMAT -> "这看起来是 .smd（交错）转储，请先转换为普通 .bin/.gen/.md。"
            NOT_MEGA_DRIVE -> "这不是 Mega Drive ROM（没有 SEGA 头）。"
            UNKNOWN_DUMP -> "不是受支持的 Metal Gear 转储。支持：v1.1（.md）和 v1.2（.gen）。"
        }
    }
    override val allowCompatible = "仍然尝试（每个补丁仍会检查原始字节）"
    override val readError = { d: String -> "无法读取文件：$d" }
    override val stepFixes = "2. 要应用的修复"
    override val fixTitle = { g: PatchGroup ->
        when (g) {
            SAVE_SYNC -> "SAVE 保存当前物品"
            Z80_TIMEOUTS -> "声音 CPU 超时"
            RAM_CLEAR -> "启动时完整清除 RAM"
            ROM_CHECK_BYPASS -> "关闭 ROM 自校验"
        }
    }
    override val fixDescription = { g: PatchGroup, profileId: String ->
        when (g) {
            SAVE_SYNC ->
                "SAVE 会保存当前的军衔、弹药、装备和武器。以前只保存最后一个检查点的快照，之后获得的物品会丢失。旧存档仍可读取。"
            Z80_TIMEOUTS ->
                "对 Z80 声音 CPU 的每一次等待在短时间后放弃，而不是永远卡在黑屏。如果 Z80 始终不响应，游戏将无声运行。"
            RAM_CLEAR ->
                if (profileId == "v1.1") "启动时清除堆栈以下的全部工作 RAM（原版在真机上会让声音驱动的变量保持随机值）。"
                else "启动时清除全部工作 RAM（原版只清除前 31 KB，声音驱动会读到其上方的随机值）。"
            ROM_CHECK_BYPASS ->
                "v1.1 会校验自身代码的校验和，只要有一个字节改变就会悄悄破坏存档签名。此项关闭该校验（v1.2 也已移除）。"
        }
    }
    override val tagRequired = "必需"
    override val tagNeededBy = { n: String -> "“$n”需要" }
    override val stepOutput = "3. 输出"
    override val outputFolder = "输出文件夹"
    override val change = "更改..."
    override val willWrite = "将写出两个相同的文件（请使用您的烧录卡菜单所列的扩展名）："
    override val willOverwrite = "同名文件已存在，将被替换。"
    override val apply = "应用修复"
    override val successTitle = "完成"
    override val successDetail = { changed: Int, checksum: String -> "已修改 $changed 个字节，头部校验和 $checksum。结果已验证（每处改动都属于某项修复，校验和有效）。" }
    override val writtenFiles = "已写出的文件"
    override val sha256Output = "修复后 ROM 的 SHA-256"
    override val failureTitle = "未写入任何内容"
    override val failure = { k: Kind, d: String ->
        when (k) {
            WRONG_SIZE -> "ROM 大小异常（$d 字节）。"
            BYTES_MISMATCH -> "$d 处的原始代码与补丁预期不符。这不是受支持版本的原始转储。"
            FREE_AREA_NOT_EMPTY -> "$d 处的空闲区域不为空，ROM 似乎已被修改。"
            AREA_TOO_SMALL -> "某个补丁例程放不进空闲区域（位于 $d）。"
            UNDECLARED_CHANGE -> "内部检查失败：字节 $d 在补丁范围之外发生了变化。"
            BAD_CHECKSUM -> "内部检查失败：头部校验和不正确。"
        }
    }
    override val writeFailure = { d: String -> "无法写出输出：$d" }
    override val nothingSelected = "请至少选择一项修复。"
    override val footer = "非官方项目，与 Konami 及移植版作者无关。不包含也不下载任何 ROM。"
    override val testedNote = "修复后的 v1.2 ROM 已在烧录卡上测试；v1.1 已在模拟器中验证。尚未在带电池的真实卡带上测试。"
    override val help = "帮助"
    override val helpTitle = "帮助"
    override val helpClose = "关闭"
    override val helpSections = listOf(
        HelpSection(
            "使用方法",
            listOf(
                "准备您自己的 Mega Drive 版 Metal Gear 转储文件（v1.1 为 .md，v1.2 为 .gen）。本程序不包含也不下载任何 ROM。",
                "点击“选择 ROM...”或把文件拖到窗口上。程序会显示识别出的版本和 SHA-256。",
                "保持修复项勾选（推荐）。必需的修复项无法取消；只要声音 CPU 超时修复需要，内存清除就保持开启。",
                "选择输出文件夹（默认是 ROM 所在文件夹），然后点击“应用修复”。程序会写出两个内容相同的文件：[fixed].gen 和 [fixed].bin。您的原始文件不会被修改。",
                "把 .bin 或 .gen 复制到您的烧录卡，或烧录到您的复刻卡带。用原始 ROM 创建的存档仍可读取。",
                "在终端中：java -jar MetalGearPatcher-*.jar --cli ROM [-o 输出] [--only save,z80,ram] [--force] [--check]",
            ),
        ),
        HelpSection(
            "运行要求",
            listOf(
                "运行程序：Java 21 或更新版本（.jar），或自带 Java 的本机安装包。修补 ROM 不需要其他任何东西。",
                "Linux：带常用 AWT 库（libxtst6、libxrender1、libfreetype6）的桌面会话（X11 或 Wayland）。使用中文界面需要 CJK 字体，例如 Noto Sans CJK。",
                "从源码构建：JDK 21+ 和仓库自带的 Gradle wrapper（它会下载 Kotlin 2.4.21、Compose Multiplatform 1.12.2、Material 3 1.9.0 和 kotlinx-coroutines 1.11.0）。",
                "重新汇编 68000 例程：binutils-m68k-linux-gnu（m68k-linux-gnu-as、ld、objcopy、nm）和 xxd。",
                "ROM 行为测试：gcc、Musashi 源码副本和 Python 3（仅标准库）。",
                "本机安装包：jpackage（JDK 自带），.deb 另需 dpkg 和 fakeroot，.msi 需要 WiX，.dmg 需要 Xcode 命令行工具。每种只能在对应系统上构建。",
            ),
        ),
        HelpSection(
            "文件列表",
            listOf(
                "输入：Metal Gear (World) (v1.2) (Aftermarket) (Unl).gen（2 MB）或 Metal Gear (v1.1).md（1.5 MB）。",
                "输出：“... [fixed].gen”和“... [fixed].bin”，内容相同，文件名随版本而定。",
                "app/：Kotlin Multiplatform 程序（core = 补丁引擎和表，ui = 界面和文字，desktopMain = 窗口和文件）。",
                "patches/：sync_inventory.s、z80_timeouts.s 和 z80_timeouts_v11.s（68000 源码）、对应的 .bin/.sym 以及 build_patches.sh。",
                "tests/：run_tests.py 和 sim/mgsim.c（ROM 行为测试）。Kotlin 测试位于 app/src/*Test/。",
                ".github/workflows/ci.yml：在 Linux、macOS 和 Windows 上构建并测试。",
                "README.md：葡萄牙语、俄语和英语的完整文档。",
            ),
        ),
    )
}
