

package org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders

import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.platform.declarations.CangJieDeclarationProvider
import org.cangnova.cangjie.analysis.api.platform.packages.CangJiePackageProvider
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProvider
import org.cangnova.cangjie.cfir.resolve.providers.CfirSymbolProviderInternals
import org.cangnova.cangjie.cfir.symbols.CfirCallableSymbol
import org.cangnova.cangjie.cfir.symbols.CfirNamedFunctionSymbol
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.psi.CjCallableDeclaration
import org.cangnova.cangjie.psi.CjClassLikeDeclaration
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.CjProperty

/**
 * 基于仓颉源码声明索引的低阶 CFIR 符号提供器。
 *
 * 该抽象层把平台侧 [CangJieDeclarationProvider] 暴露的 PSI 声明转换为 CFIR 符号，并同时保留
 * [LLPsiAwareSymbolProvider] 的 PSI 精确命中能力。模块内源码符号、组合符号提供器和按 PSI 查询的
 * 特化路径都通过该类型共享声明索引与包索引契约。
 *
 * @see org.cangnova.cangjie.analysis.low.level.api.cfir.symbolProviders.combined.LLCombinedCangJieSymbolProvider
 */
@OptIn(CaPlatformInterface::class)
internal abstract class LLCangJieSymbolProvider(session: CfirSession) :
    CfirSymbolProvider(session),
    LLKnownClassDeclarationSymbolProvider<CjClassLikeDeclaration>,
    LLPsiAwareSymbolProvider {
    /**
     * 当前符号提供器使用的平台声明索引。
     *
     * 实现必须保证该索引与所属模块内容范围一致，否则基于名字查询和基于 PSI 查询得到的符号集合会不一致。
     */
    abstract val declarationProvider: CangJieDeclarationProvider

    /**
     * 当前符号提供器使用的平台包索引。
     *
     * 包存在性查询通过该索引与 [declarationProvider] 协同完成，用于包作用域构建和顶层声明查找。
     */
    abstract val packageProvider: CangJiePackageProvider

    /**
     * 物化当前提供器需要额外公开的顶层扩展文件。
     *
     * 大部分源码符号提供器不需要额外物化文件，因此默认返回空列表；需要把扩展声明作为独立 CFIR 文件暴露的实现会覆写该方法。
     */
    internal open fun materializeTopLevelExtendFiles(): List<CfirFile> = emptyList()

    /**
     * 将已知 [callables] 对应的 [callableId] 顶层可调用符号追加到 [destination]。
     *
     * 该路径面向调用方已经持有 PSI 声明的场景，避免再次访问声明索引。传入声明必须等价于
     * [CangJieDeclarationProvider.getTopLevelFunctions] 与 [CangJieDeclarationProvider.getTopLevelProperties]
     * 对同一 [callableId] 的联合结果，保证产出的 [CfirCallableSymbol] 与普通名字查询路径一致。
     */
    @CfirSymbolProviderInternals
    abstract fun getTopLevelCallableSymbolsTo(
        destination: MutableList<CfirCallableSymbol<*>>,
        callableId: CallableId,
        callables: Collection<CjCallableDeclaration>,
    )

    /**
     * 将已知 [functions] 对应的 [callableId] 顶层函数符号追加到 [destination]。
     *
     * 该路径用于已解析出候选函数 PSI 的调用场景，传入集合必须与
     * [CangJieDeclarationProvider.getTopLevelFunctions] 对同一 [callableId] 的结果保持一致。
     */
    @CfirSymbolProviderInternals
    abstract fun getTopLevelFunctionSymbolsTo(
        destination: MutableList<CfirNamedFunctionSymbol>,
        callableId: CallableId,
        functions: Collection<CjNamedFunction>,
    )

    /**
     * 将已知 [properties] 对应的 [callableId] 顶层属性符号追加到 [destination]。
     *
     * 该路径用于已解析出候选属性 PSI 的调用场景，传入集合必须与
     * [CangJieDeclarationProvider.getTopLevelProperties] 对同一 [callableId] 的结果保持一致。
     */
    @CfirSymbolProviderInternals
    abstract fun getTopLevelPropertySymbolsTo(
        destination: MutableList<CfirPropertySymbol>,
        callableId: CallableId,
        properties: Collection<CjProperty>,
    )

    /**
     * 返回包含实现类型和所属会话的调试展示文本。
     */
    override fun toString(): String {
        return "${this::class.simpleName} for $session"
    }
}
