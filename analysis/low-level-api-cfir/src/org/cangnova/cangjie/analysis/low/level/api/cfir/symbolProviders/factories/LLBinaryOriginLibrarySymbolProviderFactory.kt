/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.factories

import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.decompiled.CaBuiltinsRootAware
import org.cangnova.cangjie.analysis.api.decompiled.CaBuiltinsVirtualFileProvider
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
    override fun createJvmLibrarySymbolProvider(
        session: LLCfirSession,
        packagePartProvider: LLPackagePartProvider,
        scope: GlobalSearchScope,
    ): List<CfirSymbolProvider> =
        createCommonLibrarySymbolProvider(session, packagePartProvider, scope)

    override fun createCommonLibrarySymbolProvider(
        session: LLCfirSession,
        packagePartProvider: LLPackagePartProvider,
        scope: GlobalSearchScope,
    ): List<CfirSymbolProvider> =
        listOf(createDeserializedLibrarySymbolProvider(session))

    override fun createBuiltinsSymbolProvider(session: LLCfirSession): List<CfirSymbolProvider> =
        listOf(
            CfirBuiltinSymbolProvider(session),
            createBuiltinsDeserializedSymbolProvider(session),
        )

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
     * builtins roots 对应的反序列化 provider。
     */
    private fun createBuiltinsDeserializedSymbolProvider(session: LLCfirSession): CfirSymbolProvider {
        val provider = CaBuiltinsVirtualFileProvider.getInstance()
        val builtinsRootProvider = provider as? CaBuiltinsRootAware
            ?: error(
                "Binary-origin builtins session requires `${CaBuiltinsRootAware::class.simpleName}` " +
                    "to recover stdlib `.cjo` roots; provider=${provider::class.qualifiedName}",
            )

        val rootPathString = builtinsRootProvider.getBuiltinRootVirtualFiles()
            .map { virtualFile -> File(virtualFile.path) }
            .map { file -> if (file.isDirectory) file else file.parentFile ?: file }
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
}
