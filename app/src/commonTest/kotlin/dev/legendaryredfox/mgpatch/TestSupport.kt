package dev.legendaryredfox.mgpatch

import dev.legendaryredfox.mgpatch.core.RomProfile

/** A ROM-shaped byte array that holds exactly the original bytes every patch of [profile] checks. */
internal fun syntheticRom(profile: RomProfile): ByteArray {
    val rom = ByteArray(profile.size)
    "SEGA MEGA DRIVE ".encodeToByteArray().copyInto(rom, 0x100)
    // plausible header fields so the checksum logic has something to start from
    rom[0x1A6] = 0x00
    profile.edits.forEach { it.expected.copyInto(rom, it.address) }
    return rom
}

/** In-memory file system. */
internal class FakeRomIo(files: Map<String, ByteArray> = emptyMap()) : RomIo {
    val files = files.toMutableMap()
    val writes = mutableListOf<String>()
    var failWrites = false

    override fun read(path: String): ByteArray = files[path] ?: error("no such file: $path")

    override fun write(path: String, data: ByteArray) {
        if (failWrites) error("disk full")
        files[path] = data
        writes += path
    }

    override fun exists(path: String): Boolean = path in files
    override fun parentOf(path: String): String = path.substringBeforeLast('/', ".")
    override fun fileName(path: String): String = path.substringAfterLast('/')
    override fun join(dir: String, name: String): String = "$dir/$name"
}
