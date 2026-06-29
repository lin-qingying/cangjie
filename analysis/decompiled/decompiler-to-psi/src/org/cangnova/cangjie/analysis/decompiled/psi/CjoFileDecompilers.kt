package org.cangnova.cangjie.analysis.decompiled.psi

import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.analysis.decompiler.stub.file.CjoStubBuilder

/**
 * 仓颉 `.cjo` binary decompiler 注册表。
 *
 * 该类型承载 `.cjo` 反编译链的 decompiler 协议。
 */
object CjoFileDecompilers {
    /**
     * 所有 `.cjo` decompiler 共享的基础协议。
     */
    interface Decompiler {
        /**
         * 判断当前 decompiler 是否支持指定二进制文件。
         */
        fun accepts(file: VirtualFile): Boolean
    }

    /**
     * 只生成反编译文本、不创建 PSI view provider 的轻量 decompiler。
     */
    abstract class Light : Decompiler {
        /**
         * 表示轻量反编译器无法从输入文件生成文本。
         */
        class CannotDecompileException(
            message: String,
            cause: Throwable,
        ) : RuntimeException(message, cause)

        /**
         * 返回指定 `.cjo` 文件的反编译文本。
         */
        abstract fun getText(file: VirtualFile): CharSequence
    }

    /**
     * 可创建 stub builder 与 PSI view provider 的完整 `.cjo` decompiler。
     */
    abstract class Full : Decompiler {
        /**
         * 返回该完整 decompiler 使用的 `.cjo` stub builder。
         */
        abstract fun getStubBuilder(): CjoStubBuilder

        /**
         * 为指定 `.cjo` 文件创建 IntelliJ PSI view provider。
         */
        abstract fun createFileViewProvider(
            file: VirtualFile,
            manager: PsiManager,
            physical: Boolean,
        ): FileViewProvider
    }

    /**
     * 仓颉 `.cjo` decompiler 的 IntelliJ 扩展点名称。
     */
    val EP_NAME: ExtensionPointName<Decompiler> = ExtensionPointName("org.cangnova.cangjie.cjoFileDecompiler")

    /**
     * 返回全局 decompiler 注册表实例。
     */
    fun getInstance(): CjoFileDecompilers = this

    /**
     * 查找第一个既属于指定 decompiler 类型、又接受目标文件的扩展实现。
     */
    fun <D : Decompiler> find(file: VirtualFile, decompilerClass: Class<D>): D? {
        return EP_NAME.extensionList.firstOrNull { decompiler ->
            decompilerClass.isInstance(decompiler) && decompiler.accepts(file)
        }?.let(decompilerClass::cast)
    }
}
