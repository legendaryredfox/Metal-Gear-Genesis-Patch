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

object MessagesRu : Messages {
    override val appTitle = "Metal Gear Genesis Patcher"
    override val appSubtitle = "Исправления для реального железа: порт Metal Gear на Mega Drive (v1.1 и v1.2)"
    override val language = "Язык"
    override val stepRom = "1. Ваш ROM"
    override val chooseRom = "Выбрать ROM..."
    override val dropHint = "или перетащите файл сюда"
    override val noRomChosen = "ROM не выбран. В программе нет ROM: используйте свой дамп."
    override val fileLabel = "Файл"
    override val sizeLabel = "Размер"
    override val hashLabel = "SHA-256"
    override val usingProfile = { p: String -> "Набор патчей: $p" }
    override val statusKnown = { p: String -> "Распознан: $p" }
    override val statusCompatible = { p: String -> "Похоже на $p, но файл не совпадает с известным дампом (другой хеш)." }
    override val statusAlreadyPatched = "В этом ROM уже есть исправления. Используйте исходный дамп."
    override val statusUnrecognized = { r: Reason ->
        when (r) {
            TOO_SMALL -> "Файл слишком мал для ROM Mega Drive."
            SMD_FORMAT -> "Похоже на дамп .smd (с чередованием). Сначала преобразуйте в обычный .bin/.gen/.md."
            NOT_MEGA_DRIVE -> "Это не ROM Mega Drive (нет заголовка SEGA)."
            UNKNOWN_DUMP -> "Неподдерживаемый дамп Metal Gear. Поддерживаются: v1.1 (.md) и v1.2 (.gen)."
        }
    }
    override val allowCompatible = "Всё равно попробовать (каждый патч всё ещё проверяет исходные байты)"
    override val readError = { d: String -> "Не удалось прочитать файл: $d" }
    override val stepFixes = "2. Применяемые исправления"
    override val fixTitle = { g: PatchGroup ->
        when (g) {
            SAVE_SYNC -> "SAVE сохраняет текущие предметы"
            Z80_TIMEOUTS -> "Тайм-ауты звукового процессора"
            RAM_CLEAR -> "Полная очистка RAM при загрузке"
            ROM_CHECK_BYPASS -> "Отключить контрольную сумму ROM"
        }
    }
    override val fixDescription = { g: PatchGroup, profileId: String ->
        when (g) {
            SAVE_SYNC ->
                "SAVE записывает текущие звание, патроны, снаряжение и оружие. Раньше записывался только снимок последнего чекпоинта, и полученные позже предметы терялись. Старые сохранения продолжают загружаться."
            Z80_TIMEOUTS ->
                "Ожидания звукового процессора Z80 прекращаются по тайм-ауту, а не зависают навсегда на чёрном экране. Если Z80 не отвечает, игра идёт без звука."
            RAM_CLEAR ->
                if (profileId == "v1.1") "Очищает всю рабочую RAM ниже стека при загрузке (оригинал оставляет переменные звукового драйвера случайными на реальном железе)."
                else "Очищает всю рабочую RAM при загрузке (оригинал очищал только первые 31 КБ; звуковой драйвер читал случайные значения выше)."
            ROM_CHECK_BYPASS ->
                "v1.1 проверяет контрольную сумму собственного кода и молча портит подпись сохранения, если изменился хоть один байт. Это отключает проверку (в v1.2 её тоже убрали)."
        }
    }
    override val tagRequired = "обязательно"
    override val tagNeededBy = { n: String -> "нужно для \"$n\"" }
    override val stepOutput = "3. Результат"
    override val outputFolder = "Папка вывода"
    override val change = "Изменить..."
    override val willWrite = "Будут записаны два одинаковых файла (берите расширение, которое показывает меню вашего флеш-картриджа):"
    override val willOverwrite = "Файл с таким именем уже есть и будет заменён."
    override val apply = "Применить исправления"
    override val successTitle = "Готово"
    override val successDetail = { changed: Int, checksum: String -> "Изменено байт: $changed, контрольная сумма заголовка $checksum. Результат проверен (каждое изменение относится к исправлению, сумма верна)." }
    override val writtenFiles = "Записанные файлы"
    override val sha256Output = "SHA-256 исправленного ROM"
    override val failureTitle = "Ничего не записано"
    override val failure = { k: Kind, d: String ->
        when (k) {
            WRONG_SIZE -> "Неожиданный размер ROM ($d байт)."
            BYTES_MISMATCH -> "Исходный код по адресу $d не тот, что ожидает патч. Это не нетронутый дамп поддерживаемой версии."
            FREE_AREA_NOT_EMPTY -> "Свободная область по адресу $d не пуста, ROM, похоже, уже изменён."
            AREA_TOO_SMALL -> "Процедура патча не помещается в свободную область (по адресу $d)."
            UNDECLARED_CHANGE -> "Внутренняя проверка не пройдена: байт $d изменился вне диапазонов патча."
            BAD_CHECKSUM -> "Внутренняя проверка не пройдена: контрольная сумма заголовка неверна."
        }
    }
    override val writeFailure = { d: String -> "Не удалось записать результат: $d" }
    override val nothingSelected = "Выберите хотя бы одно исправление."
    override val footer = "Неофициальный проект, не связан с Konami и авторами порта. ROM не включены и не загружаются."
    override val testedNote = "Исправленный ROM v1.2 проверен на флеш-картридже; v1.1 проверен в симуляторе и эмуляторах. На настоящем картридже с батареей не проверялось."
    override val help = "Справка"
    override val helpTitle = "Справка"
    override val helpClose = "Закрыть"
    override val helpSections = listOf(
        HelpSection(
            "Как пользоваться",
            listOf(
                "Возьмите свой дамп Metal Gear для Mega Drive (v1.1 в виде .md или v1.2 в виде .gen). ROM не включены и не загружаются.",
                "Нажмите «Выбрать ROM...» или перетащите файл в окно. Программа покажет распознанную версию и SHA-256.",
                "Оставьте исправления отмеченными (рекомендуется). Обязательные нельзя снять, а очистка RAM остаётся включённой, пока от неё зависят тайм-ауты звукового процессора.",
                "Выберите папку вывода (по умолчанию папка ROM) и нажмите «Применить исправления». Записываются два одинаковых файла, [fixed].gen и [fixed].bin. Исходный файл никогда не изменяется.",
                "Скопируйте .bin или .gen на флеш-картридж или прошейте в репро-картридж. Сохранения, сделанные в исходной ROM, продолжают загружаться.",
                "Из терминала: java -jar MetalGearPatcher-*.jar --cli ROM [-o ВЫХОД] [--only save,z80,ram] [--force] [--check]",
            ),
        ),
        HelpSection(
            "Требования",
            listOf(
                "Для запуска программы: Java 21 или новее (для .jar) или нативный установщик со своей Java. Для исправления ROM больше ничего не нужно.",
                "Linux: графический сеанс (X11 или Wayland) с обычными библиотеками AWT (libxtst6, libxrender1, libfreetype6). Для китайского интерфейса нужен шрифт CJK, например Noto Sans CJK.",
                "Для сборки из исходников: JDK 21+ и входящая в репозиторий обёртка Gradle (она скачает Kotlin 2.4.21, Compose Multiplatform 1.12.2, Material 3 1.9.0 и kotlinx-coroutines 1.11.0).",
                "Для пересборки процедур 68000: binutils-m68k-linux-gnu (m68k-linux-gnu-as, ld, objcopy, nm) и xxd.",
                "Для тестов поведения ROM: gcc, клон Musashi и Python 3 (только стандартная библиотека).",
                "Для нативных установщиков: jpackage (входит в JDK), а также dpkg и fakeroot для .deb, WiX для .msi, инструменты командной строки Xcode для .dmg. Каждый собирается только в своей системе.",
            ),
        ),
        HelpSection(
            "Файлы",
            listOf(
                "Вход: Metal Gear (World) (v1.2) (Aftermarket) (Unl).gen (2 МБ) или Metal Gear (v1.1).md (1,5 МБ).",
                "Выход: «... [fixed].gen» и «... [fixed].bin» с одинаковым содержимым, имя зависит от версии.",
                "app/: программа на Kotlin Multiplatform (core = движок и таблицы патчей, ui = экран и тексты, desktopMain = окно и файлы).",
                "patches/: sync_inventory.s, z80_timeouts.s и z80_timeouts_v11.s (исходники 68000), соответствующие .bin/.sym и build_patches.sh.",
                "tests/: run_tests.py и sim/mgsim.c (тесты поведения ROM). Тесты на Kotlin лежат в app/src/*Test/.",
                ".github/workflows/ci.yml: сборка и тесты в Linux, macOS и Windows.",
                "README.md: полная документация на португальском, русском и английском.",
            ),
        ),
    )
}
