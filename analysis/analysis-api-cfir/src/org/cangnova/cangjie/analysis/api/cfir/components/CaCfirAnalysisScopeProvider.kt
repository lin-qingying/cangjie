package org.cangnova.cangjie.analysis.api.cfir.components

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaPlatformInterface
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaAnalysisScopeProvider
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseAnalysisScopeProviderEx
import org.cangnova.cangjie.analysis.api.impl.base.components.CaBaseSessionComponent
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaResolutionScope
import org.cangnova.cangjie.parsing.compilationParsingResultOrNull
import org.cangnova.cangjie.psi.CjFile

/**
 * 对齐 Kotlin 的 analysis-scope provider 落位，
 * 只负责把模块内容作用域暴露给 Analysis API。
 */
@OptIn(CaImplementationDetail::class, CaPlatformInterface::class)
internal class CaCfirAnalysisScopeProvider(
    /**
     * 延迟取得当前 CFIR Analysis session。
     */
    override val analysisSessionProvider: () -> CaCfirSession,
    /**
     * 当前 session 对应模块可被分析的解析作用域。
     */
    private val resolutionScope: CaResolutionScope,

) : CaBaseSessionComponent<CaCfirSession>(), CaBaseAnalysisScopeProviderEx {

    @OptIn(CaPlatformInterface::class)
    /**
     * 暴露给 Analysis API 调用方的全局搜索作用域。
     */
    override val analysisScope: GlobalSearchScope
        get() = withValidityAssertion { resolutionScope }

    /**
     * 判断 PSI 元素是否处于当前解析作用域内。
     */
    override fun canBeAnalysedImpl(element: PsiElement): Boolean {
        // target只是本代同一源码的编译产品；资格仍按真实原PSI的现有模块/作用域检查。
        // 缺少唯一导航时不授予资格，不能把普通副本或未知来源节点当作同模块输入。
        val product = (element.containingFile as? CjFile)?.compilationParsingResultOrNull()
        val source = if (product != null) product.originalElementOrNull(element) ?: return false else element
        return resolutionScope.contains(source)
    }


}
