package org.cangnova.cangjie.macro.file

import org.cangnova.cangjie.lang.CangJieMacroCallLanguage
import org.cangnova.cangjie.psi.CjFile
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import javax.swing.Icon

class CjMacroCallFile(
    private val provider: FileViewProvider,
) : CjFile(
    provider,
    isCompiled = true
) {
    override fun toString(): String {
        return "CjMacroCallFile File: $name"
    }

    override fun getFileType(): FileType {
        return CangJieMacroCallFileType
    }

    companion object {
        @JvmStatic
        fun isUserVisible(): Boolean = Registry.`is`("cangjie.macro.expansion.file.visible", false)

        @JvmStatic
        fun isMacroCallFile(file: VirtualFile): Boolean = file.name.endsWith(".cj.macrocall")
    }
}

object CangJieMacroCallFileType : LanguageFileType(CangJieMacroCallLanguage) {

    val EXTENSION: String = "cj.macrocall"

    override fun getDisplayName(): String {
        return EXTENSION
    }

    override fun getName() = EXTENSION

    override fun getDescription(): String = DEFAULT_DESCRIPTION

    override fun getDefaultExtension() = EXTENSION

    override fun getIcon(): Icon? = null

    override fun isReadOnly() = true

    override fun getCharset(file: VirtualFile, content: ByteArray): String? = null

    private val DEFAULT_DESCRIPTION = "CangJie Macro Call"
}
