

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.factories

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.analysis.decompiler.stub.file.cjoSearchPathStringOf
import org.cangnova.cangjie.analysis.decompiler.stub.file.distinctCjoSearchRoots
import org.cangnova.cangjie.analysis.decompiler.stub.file.normalizeCjoSearchRoot
import org.cangnova.cangjie.analysis.decompiler.stub.file.toCjoSearchRoot
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.projectStructure.moduleData
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.cfir.resolve.providers.CfirBuiltinSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.serialization.cjo.CjoManager
import org.cangnova.cangjie.cfir.serialization.cjo.CjoSearchPath
import org.cangnova.cangjie.cfir.serialization.provider.CfirDeserializedSymbolProvider
import org.cangnova.cangjie.cfir.session.cangjieScopeProvider
import java.io.File

/**
 * [LLLibrarySymbolProviderFactory] for [KotlinDeserializedDeclarationsOrigin.BINARIES][org.cangnova.cangjie.analysis.api.platform.KotlinDeserializedDeclarationsOrigin.BINARIES].
 */
internal object LLBinaryOriginLibrarySymbolProviderFactory : LLLibrarySymbolProviderFactory {
    /**
     * binary-origin JVM library provider 当前复用 common library provider 创建路径。
     */
    override fun createJvmLibrarySymbolProvider(
        session: LLCfirSession,
        packagePartProvider: LLPackagePartProvider,
        scope: GlobalSearchScope,
    ): List<CfirSymbolProvider> =
        createCommonLibrarySymbolProvider(session, packagePartProvider, scope)

    /**
     * 创建基于 `.cjo` 反序列化的 common library symbol provider。
     */
    override fun createCommonLibrarySymbolProvider(
        session: LLCfirSession,
        packagePartProvider: LLPackagePartProvider,
        scope: GlobalSearchScope,
    ): List<CfirSymbolProvider> =
        listOf(createDeserializedLibrarySymbolProvider(session))

    /**
     * 创建 builtins session 使用的 `.cjo` 反序列化 provider 与 synthetic builtin provider。
     *
     * `.cjo` 中的真实 BuiltInDecl 必须优先于 synthetic 声明；后者只负责补缺。
     */
    override fun createBuiltinsSymbolProvider(session: LLCfirSession): List<CfirSymbolProvider> =
        listOf(
            createBuiltinsDeserializedSymbolProvider(session),
            CfirBuiltinSymbolProvider(session),
        )

    /**
     * 创建普通 library `.cjo` 反序列化 symbol provider。
     */
    private fun createDeserializedLibrarySymbolProvider(session: LLCfirSession): CfirSymbolProvider {
        val librarySearchPaths = (session.caModule as? CaLibraryModule)
            ?.binaryRoots
            .orEmpty()
            .mapNotNull { item -> item.virtualFile }
            .mapNotNull(VirtualFile::asCjoSearchRoot)
            .distinctCjoSearchRoots()

        return CfirDeserializedSymbolProvider(
            session = session,
            cjoManager = CjoManager(CjoSearchPath(additionalLibrarySearchPaths = librarySearchPaths)),
            cangjieScopeProvider = session.cangjieScopeProvider,
            libraryModuleData = session.moduleData,
        )
    }

    /**
     * 仓颉的 builtins session 需要同时覆盖两类符号：
     * 1. 真正的 primitive / builtin provider；
     * 2. `std.core` 等 stdlib `.cjo` 中定义的核心类型（例如 `String`）。
     *
     * Kotlin 的 binary-origin builtins session 只保留 builtins provider 即可，
     * 但仓颉的 `String` 不属于 primitive builtins，因此这里必须额外接入
     * builtins `.cjo` 搜索根对应的反序列化 provider。
     */
    private fun createBuiltinsDeserializedSymbolProvider(session: LLCfirSession): CfirSymbolProvider {
        val rootPathString = cjoSearchPathStringOf(
            BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles(session.project).map(::toCjoSearchRoot)
        )

        return CfirDeserializedSymbolProvider(
            session = session,
            cjoManager = CjoManager(
                CjoSearchPath { key ->
                    when (key) {
                        "CANGJIE_LIBRARY", "CANGJIE_STDLIB_MODULE" -> rootPathString
                        else -> null
                    }
                },
            ),
            cangjieScopeProvider = session.cangjieScopeProvider,
            libraryModuleData = session.moduleData,
        )
    }
}

/** 将 library 二进制根统一为 CJO 搜索目录。 */
private fun VirtualFile.asCjoSearchRoot(): File? =
    normalizeCjoSearchRoot(File(path)).takeIf(File::isDirectory)
