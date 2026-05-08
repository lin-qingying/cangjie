package org.cangnova.cangjie.psi

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.StubBuilder
import com.intellij.openapi.util.Key
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.cangnova.cangjie.psi.stubs.CangJieFileStub

/**
 * `.cjo` 等编译产物恢复出的只读 PSI 文件。
 *
 * 它的真源是二进制元数据，而不是可编辑源码，因此需要显式持有：
 * 1. compiled stub builder；
 * 2. decompiled 文本提供器。
 */
open class CjDecompiledFile(
    viewProvider: FileViewProvider,
    private val decompiledStubBuilder: StubBuilder,
    private val decompiledTextProvider: () -> String,
) : CjFile(viewProvider, isCompiled = true) {
    companion object {
        private val ORIGINAL_STUB_CHILDREN_KEY = Key.create<List<String>>("cangjie.decompiled.originalStubChildren")
        private val COPIED_STUB_CHILDREN_KEY = Key.create<List<String>>("cangjie.decompiled.copiedStubChildren")
        private val DECLARATION_DEBUG_KEY = Key.create<List<String>>("cangjie.decompiled.declarationDebug")
    }

    /**
     * 对齐 Kotlin `KtDecompiledFile` 的 compiled-stub 优先语义：
     * decompiled PSI 的文件级查询必须先读取 compiled stub，
     * 不能退化成基于反编译文本的 AST 重建。
     */
    private val compiledFileStub: CangJieFileStub by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        decompiledStubBuilder.buildStubTree(this) as CangJieFileStub
    }

    override val customStubBuilder: StubBuilder
        get() = decompiledStubBuilder

    override val greenStub: CangJieFileStub?
        get() = super.greenStub ?: compiledFileStub

    /**
     * `.cjo` decompiled PSI 的文件身份必须直接暴露到底层 binary VirtualFile，
     * 否则 project-structure 无法像 Kotlin decompiled PSI 一样按 `containingFile.virtualFile`
     * 恢复 builtins / library module。
     */
    override fun getVirtualFile(): VirtualFile = viewProvider.virtualFile

    override fun getFileType(): FileType = CangJieBuiltInFileType

    override fun getText(): String = decompiledTextProvider()

    fun debugCompiledStubChildren(): List<String> {
        return compiledFileStub.childrenStubs.map { child ->
            "${child::class.simpleName}:${child.stubType}"
        }
    }

    fun debugOriginalStubChildren(): List<String> = getUserData(ORIGINAL_STUB_CHILDREN_KEY).orEmpty()

    fun debugCopiedStubChildren(): List<String> = getUserData(COPIED_STUB_CHILDREN_KEY).orEmpty()

    fun debugDeclarationKinds(): List<String> = getUserData(DECLARATION_DEBUG_KEY).orEmpty()

    fun recordCompiledStubDebug(originalChildren: List<String>, copiedChildren: List<String>, declarationKinds: List<String>) {
        putUserData(ORIGINAL_STUB_CHILDREN_KEY, originalChildren)
        putUserData(COPIED_STUB_CHILDREN_KEY, copiedChildren)
        putUserData(DECLARATION_DEBUG_KEY, declarationKinds)
    }

    override fun toString(): String = "CangJie Decompiled File: $name"
}
