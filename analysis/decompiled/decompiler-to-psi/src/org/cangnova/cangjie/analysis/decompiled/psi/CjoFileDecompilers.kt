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
    interface Decompiler {
        fun accepts(file: VirtualFile): Boolean
    }

    abstract class Light : Decompiler {
        class CannotDecompileException(
            message: String,
            cause: Throwable,
        ) : RuntimeException(message, cause)

        abstract fun getText(file: VirtualFile): CharSequence
    }

    abstract class Full : Decompiler {
        abstract fun getStubBuilder(): CjoStubBuilder

        abstract fun createFileViewProvider(
            file: VirtualFile,
            manager: PsiManager,
            physical: Boolean,
        ): FileViewProvider
    }

    val EP_NAME: ExtensionPointName<Decompiler> = ExtensionPointName("org.cangnova.cangjie.cjoFileDecompiler")

    fun getInstance(): CjoFileDecompilers = this

    fun <D : Decompiler> find(file: VirtualFile, decompilerClass: Class<D>): D? {
        return EP_NAME.extensionList.firstOrNull { decompiler ->
            decompilerClass.isInstance(decompiler) && decompiler.accepts(file)
        }?.let(decompilerClass::cast)
    }
}
