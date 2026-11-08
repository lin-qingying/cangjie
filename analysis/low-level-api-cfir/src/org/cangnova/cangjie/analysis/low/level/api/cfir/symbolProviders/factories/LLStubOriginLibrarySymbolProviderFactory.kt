

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.factories

import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.sessions.LLCfirSession
import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.BuiltinsDeserializedContainerSourceProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.NullDeserializedContainerSourceProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.StubAndBuiltinsDeserializedContainerSourceProvider
import org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.LLCangJieStubBasedLibrarySymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirBuiltinSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider

/**
 * [LLLibrarySymbolProviderFactory] for [KotlinDeserializedDeclarationsOrigin.STUBS][org.cangnova.cangjie.analysis.api.platform.KotlinDeserializedDeclarationsOrigin.STUBS].
 */
internal object LLStubOriginLibrarySymbolProviderFactory : LLLibrarySymbolProviderFactory {
    /**
     * 创建基于 compiled PSI/stub 的 JVM library symbol provider。
     */
    override fun createJvmLibrarySymbolProvider(
        session: LLCfirSession,
        packagePartProvider: LLPackagePartProvider,
        scope: GlobalSearchScope,
    ): List<CfirSymbolProvider> {
        return listOf(
            LLCangJieStubBasedLibrarySymbolProvider(
                session,
                StubAndBuiltinsDeserializedContainerSourceProvider,
                { scope },
            )
        )
    }

    /**
     * 创建基于 compiled PSI/stub 的 common library symbol provider。
     */
    override fun createCommonLibrarySymbolProvider(
        session: LLCfirSession,
        packagePartProvider: LLPackagePartProvider,
        scope: GlobalSearchScope,
    ): List<CfirSymbolProvider> = listOf(
        LLCangJieStubBasedLibrarySymbolProvider(session, NullDeserializedContainerSourceProvider, { scope }),
    )

    /**
     * 创建基于 builtins compiled PSI/stub 的 builtins symbol provider。
     *
     * 与 BINARIES 工厂（[LLBinaryOriginLibrarySymbolProviderFactory]）保持同一装配契约：
     * stub 反序列化 provider 只覆盖 `.cjo` 真实导出的声明（`AtomicInt64` 等派生名），
     * **`Int64` / `UInt8` 等 primitive 的合成声明只由 [CfirBuiltinSymbolProvider] 暴露**
     * （`CfirBuiltinSymbolProviderTest` 实证）。缺了它，IDE（默认 STUBS 模式）里
     * 表达式位置 `UInt8.fromLE()` 这类 primitive qualifier 的名称解析整段失效——
     * 类型位置因 `basicTypeOf` 直映射不经 provider，所以表现上只有表达式爆红。
     */
    override fun createBuiltinsSymbolProvider(session: LLCfirSession): List<CfirSymbolProvider> {
        return listOf(
            LLCangJieStubBasedLibrarySymbolProvider(
                session,
                BuiltinsDeserializedContainerSourceProvider,
                // builtins 搜索作用域每次访问都会重新遍历 SDK 目录，只能在首次查询声明时创建。
                { BuiltinsVirtualFileProvider.getInstance().createBuiltinsScope(session.project) },
            ),
            CfirBuiltinSymbolProvider(session),
        )
    }
}
