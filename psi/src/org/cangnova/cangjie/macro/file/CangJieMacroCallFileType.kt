package org.cangnova.cangjie.macro.file

import org.cangnova.cangjie.lang.CangJieMacroCallLanguage
import org.cangnova.cangjie.psi.CjFile
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import javax.swing.Icon

/**
 * 表示 `CjMacroCallFile`，承载宏文件 PSI中的语法节点、索引桩或辅助模型。
 */
class CjMacroCallFile(
    /**
     * 保存 `provider` 的内部状态，供宏文件 PSI实现维护节点缓存或解析上下文。
     */
    private val provider: FileViewProvider,
) : CjFile(
    provider,
    isCompiled = true
) {
    /**
     * 实现 `toString` 的宏文件 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun toString(): String {
        return "CjMacroCallFile File: $name"
    }

    /**
     * 实现 `getFileType` 的宏文件 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
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

/**
 * 提供 `CangJieMacroCallFileType` 单例，集中承载宏文件 PSI的共享状态、工厂或工具行为。
 */
object CangJieMacroCallFileType : LanguageFileType(CangJieMacroCallLanguage) {

    /**
     * 保存 `EXTENSION`，供宏文件 PSI流程读取节点结构或语义信息。
     */
    val EXTENSION: String = "cj.macrocall"

    /**
     * 实现 `getDisplayName` 的宏文件 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getDisplayName(): String {
        return EXTENSION
    }

    /**
     * 实现 `getName` 的宏文件 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getName() = EXTENSION

    /**
     * 实现 `getDescription` 的宏文件 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getDescription(): String = DEFAULT_DESCRIPTION

    /**
     * 实现 `getDefaultExtension` 的宏文件 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getDefaultExtension() = EXTENSION

    /**
     * 实现 `getIcon` 的宏文件 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getIcon(): Icon? = null

    /**
     * 实现 `isReadOnly` 的宏文件 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun isReadOnly() = true

    /**
     * 实现 `getCharset` 的宏文件 PSI协议回调，保持与 IntelliJ PSI 访问契约一致。
     */
    override fun getCharset(file: VirtualFile, content: ByteArray): String? = null

    /**
     * 保存 `DEFAULT_DESCRIPTION` 的内部状态，供宏文件 PSI实现维护节点缓存或解析上下文。
     */
    private val DEFAULT_DESCRIPTION = "CangJie Macro Call"
}
