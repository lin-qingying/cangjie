

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders

import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.StubAndBuiltinsDeserializedContainerSourceProvider
import org.cangnova.cangjie.cfir.caches.cfirCachesFactory
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationOrigin
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.stubs.CangJieFileStubKind
import org.cangnova.cangjie.psi.stubs.impl.CangJieStubOrigin
import org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization.compiledStub

/**
 * 为合成多文件类 part 中的顶层 callable 提供兜底符号反序列化。
 *
 * 多文件类 part 可能不会进入常规索引，但 IDE 仍可能为其构建 stub，并在库 callable 查找中请求对应符号。该提供器只在主库符号
 * 查询没有结果时被调用，用于按已知 PSI 声明补齐函数或属性符号。
 *
 * @property session 用于创建缓存并执行 stub 反序列化的 CFIR 会话。
 * @see addCallableIfNeeded
 **/
internal class LLCangJieStubBasedLibraryMultifileClassPartCallableSymbolProvider(val session: CfirSession) {
    /**
     * 多文件类 part 函数兜底缓存。
     */
    private val fallbackFunctionCache = session.cfirCachesFactory.createCache(::loadFunction)
    /**
     * 多文件类 part 属性兜底缓存。
     */
    private val fallbackPropertyCache = session.cfirCachesFactory.createCache(::loadProperty)

    /**
     * 在需要时把 [callableDeclaration] 对应的多文件类 part callable 符号追加到 [callableCandidates]。
     *
     * 只有声明所在文件 stub 表示多文件类 facade 时才会尝试加载。该兜底路径用于索引缺失但 stub 存在的库 callable，
     * 不参与普通源码或普通库文件的主查询流程。
     */
    fun addCallableIfNeeded(
        callableCandidates: MutableList<CfirCallableSymbol<*>>,
        packageFqName: FqName,
        shortName: Name,
        callableDeclaration: CjCallableDeclaration,
    ) {
        val fileStubKind = callableDeclaration.containingCjFile.stub?.kind
        if (fileStubKind !is CangJieFileStubKind.WithPackage.Facade.MultifileClass) {
            return
        }

        val callableId = CallableId(packageFqName, shortName)
        val symbol = when (callableDeclaration) {
            is CjNamedFunction -> fallbackFunctionCache.getValue(callableDeclaration, callableId)
            is CjProperty -> fallbackPropertyCache.getValue(callableDeclaration, callableId)
            else -> null
        }

        symbol?.let(callableCandidates::add)
    }

    /**
     * 从多文件类 part 的 [function] stub 反序列化顶层函数符号。
     */
    private fun loadFunction(function: CjNamedFunction, callableId: CallableId): CfirNamedFunctionSymbol? {
        return LLCangJieStubBasedLibrarySymbolProvider.loadFunction(
            function = function,
            callableId = callableId,
            functionOrigin = CfirDeclarationOrigin.Library,
            deserializedContainerSourceProvider = StubAndBuiltinsDeserializedContainerSourceProvider,
            session = session,
        )
    }

    /**
     * 从多文件类 part 的 [property] stub 反序列化顶层属性符号。
     */
    private fun loadProperty(property: CjProperty, callableId: CallableId): CfirPropertySymbol? {
        return LLCangJieStubBasedLibrarySymbolProvider.loadProperty(
            property = property,
            callableId = callableId,
            propertyOrigin = CfirDeclarationOrigin.Library,
            deserializedContainerSourceProvider = StubAndBuiltinsDeserializedContainerSourceProvider,
            session = session,
        )
    }
}
