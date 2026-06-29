package org.cangnova.cangjie.lang

import com.intellij.openapi.fileTypes.LanguageFileType
import org.cangnova.cangjie.icon.CangJieIconProviderService
import javax.swing.Icon

/**
 * 表示 `CangJieFileType`，承载仓颉语言文件类型中的语法节点、索引桩或辅助模型。
 */
open class CangJieFileType : LanguageFileType(CangJieLanguage) {
    /**
     * 保存 `myIcon` 的内部状态，供仓颉语言文件类型实现维护节点缓存或解析上下文。
     */
    private val myIcon: Icon? by lazy { CangJieIconProviderService.getInstance().getFileIcon() }

    /**
     * 实现 `getName` 的仓颉语言文件类型协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getName(): String = CangJieLanguage.displayName

    /**
     * 实现 `getDescription` 的仓颉语言文件类型协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getDescription(): String = name

    /**
     * 实现 `getDefaultExtension` 的仓颉语言文件类型协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getDefaultExtension(): String = EXTENSION

    /**
     * 实现 `getIcon` 的仓颉语言文件类型协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getIcon(): Icon? = myIcon

    companion object {
        const val EXTENSION: String = "cj"

        val DOT_DEFAULT_EXTENSION: String = ".$EXTENSION"
        val INSTANCE = CangJieFileType()
    }
}
