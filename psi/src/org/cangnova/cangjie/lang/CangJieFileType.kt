package org.cangnova.cangjie.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import org.cangnova.cangjie.icon.CangJieIconProviderService
import javax.swing.Icon

open class CangJieFileType : LanguageFileType(CangJieLanguage) {
    private val myIcon: Icon? by lazy { CangJieIconProviderService.getInstance().getFileIcon() }

    override fun getName(): String = CangJieLanguage.displayName

    override fun getDescription(): String = name

    override fun getDefaultExtension(): String = EXTENSION

    override fun getIcon(): Icon? = myIcon

    companion object {
        const val EXTENSION: String = "cj"

        val DOT_DEFAULT_EXTENSION: String = ".$EXTENSION"
        val INSTANCE = CangJieFileType()
    }
}
