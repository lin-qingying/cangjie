
package org.cangnova.cangjie.analysis.low.level.api.cfir.compile

import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.low.level.api.cfir.LLCfirInternals
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirCodeFragment
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirPatternVariable
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirValueParameter
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.CfirLazyBlock
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent
import org.cangnova.cangjie.cfir.scopes.impl.CfirLocalScope
import org.cangnova.cangjie.cfir.symbols.CfirPropertySymbol
import org.cangnova.cangjie.psi.CjCodeFragment

@LLCfirInternals
/**
 * 从 CFIR session 取得 code fragment 额外作用域 provider。
 */
val CfirSession.codeFragmentScopeProvider: CodeFragmentScopeProvider by CfirSession.sessionComponentAccessor()


/**
 * 标记属性 symbol 是否表示外部环境值；仓颉当前没有 foreign value 语义。
 */
val CfirPropertySymbol.isForeignValue: Boolean
    get() = false


/**
 * 为 code fragment 提供额外 local scope 的 session component。
 */
class CodeFragmentScopeProvider(
    /**
     * 该 provider 所属的 CFIR session。
     */
    private val session: CfirSession,
) : CfirSessionComponent {
    /**
     * 收集片段**自身**的顶层声明，构成一个成员作用域。
     *
     * 仓颉有代码片段但没有脚本文件，因此这里**不**引入对标 Kotlin `ScriptMemberScope`
     * 的脚本语义，而是按片段这一真实语言特性建模：片段里写下的顶层声明对片段内表达式可见，
     * 公开层对应 `CaScopeKind.CodeFragmentMemberScope`。
     *
     * 表达式片段与类型片段没有自己的顶层声明，返回空列表——那是**正确结论**而非桩：
     * 「片段内看不见宿主专有符号」正是靠这里什么都不加来保证的。
     *
     * 片段的上下文（宿主文件、宿主接收者）由 [resolveCodeFragmentContext] 经
     * `ContextCollector` 单独提供，本方法**只**负责片段自身，两者不得互相顶替。
     */
    fun getExtraScopes(codeFragment: CjCodeFragment, cfirCodeFragment: CfirCodeFragment): List<CfirLocalScope> {
        // 片段自身声明由 `BodyResolveContext.withCodeFragment` 在 body 解析时发布成
        // `CODE_FRAGMENT_MEMBER_SCOPE`，不在这里重复发布。
        //
        // 本方法在片段 body 解析**之前**被调用（`resolveCodeFragmentContext` 先建上下文、
        // 再解析片段），此时 `block` 往往还是 `CfirLazyBlock` 桩，访问 `statements` 会直接
        // `error("CfirLazyBlock should be resolved before accessing")`。
        // 因此这里只负责**已解析**片段的成员作用域；桩态一律返回空列表，那是正确结论而非缺失。
        val block = cfirCodeFragment.block
        if (block is CfirLazyBlock) return emptyList()

        // `CfirVariable` 同时是 `CfirStatement`，所以片段自身声明就挂在 block 的 statements 上。
        val declarations = block.statements.filterIsInstance<CfirDeclaration>()
        if (declarations.isEmpty()) return emptyList()

        var scope = CfirLocalScope.forCodeFragment(session)
        for (declaration in declarations) {
            scope = when (declaration) {
                is CfirNamedFunction -> scope.storeFunction(declaration, session)
                is CfirProperty -> scope.storeProperty(declaration, session)
                // CfirTypeAlias 继承 CfirClassLikeDeclaration，此处一并覆盖。
                is CfirClassLikeDeclaration -> scope.storeClassOrTypeAlias(declaration, session)
                // CfirPatternVariable 不能直接存储：CfirLocalScope.storeVariable 对它显式 error，
                // 因为真正进入作用域的是 pattern 内部的 binding variable。
                is CfirPatternVariable -> scope
                is CfirValueParameter -> scope.storeVariable(declaration, session)
                is CfirVariable -> scope.storeVariable(declaration, session)
                else -> scope
            }
        }
        return listOf(scope)
    }
}
