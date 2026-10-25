// SPDX-License-Identifier: GPL-3.0-only

package app.tellyscope.data

import java.io.File
import java.util.Properties

/** Whether a Remote key press vibrates, kept in [file]. On until the user turns it off. */
class KeyHaptics(private val file: File) {
    @get:Synchronized @set:Synchronized
    var on: Boolean
        get() = props().getProperty("off") != "true"
        set(on) {
            val p = props().apply { if (on) remove("off") else setProperty("off", "true") }
            runCatching {
                file.parentFile?.mkdirs()
                file.outputStream().use { p.store(it, null) }
            }
        }

    private fun props() = Properties().apply {
        runCatching { file.takeIf { it.exists() }?.inputStream()?.use(::load) }
    }
}
