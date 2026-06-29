

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.factories

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.analysis.decompiler.stub.file.CjoBinaryFileReader
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
     * 创建 builtins session 使用的 primitive provider 与 `.cjo` 反序列化 provider。
     */
    override fun createBuiltinsSymbolProvider(session: LLCfirSession): List<CfirSymbolProvider> =
        listOf(
            CfirBuiltinSymbolProvider(session),
            createBuiltinsDeserializedSymbolProvider(session),
        )

    /**
     * 创建普通 library `.cjo` 反序列化 symbol provider。
     */
    private fun createDeserializedLibrarySymbolProvider(session: LLCfirSession): CfirSymbolProvider =
        CfirDeserializedSymbolProvider(
            session = session,
            cjoManager = CjoManager(CjoSearchPath()),
            cangjieScopeProvider = session.cangjieScopeProvider,
            libraryModuleData = session.moduleData,
        )

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
        val rootPathString = BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles(session.project)
            .map(::toBuiltinsSearchRoot)
            .distinctBy(File::getAbsolutePath)
            .joinToString(File.pathSeparator) { root -> root.absolutePath }

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

    /**
     * 从 builtins virtual file 推断 `.cjo` 搜索根目录。
     */
    private fun toBuiltinsSearchRoot(virtualFile: VirtualFile): File {
        val file = File(virtualFile.path)
        val parent = file.parentFile ?: return file
        val firstPackageSegment = CjoBinaryFileReader.readPackageFqName(virtualFile)?.pathSegments()?.firstOrNull()
        return if (firstPackageSegment != null && parent.name == firstPackageSegment.asString()) {
            parent.parentFile ?: parent
        } else {
            parent
        }
    }
}
