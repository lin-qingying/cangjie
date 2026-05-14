package org.cangnova.cangjie.analysis.api.components

import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.lifetime.CaLifetimeOwner
import org.cangnova.cangjie.analysis.api.lifetime.CaSessionComponent
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * 分析作用域协议。
 *
 * 设计要点/职责:
 * - 限定当前 `CaSession` 能够分析的文件集合,只有落在该作用域内的声明才能被解析为 symbol。
 * - 既用于判定可分析性,也作为下游搜索/索引能力的边界。
 *
 * 对齐 Kotlin Analysis API 的 `KaAnalysisScopeProvider`。
 */
interface CaAnalysisScopeProvider : CaSessionComponent {
    /**
     * 当前 session 覆盖的全局搜索作用域;
     * 落在该作用域之外的声明不会被构建出对应的 symbol。
     */
    val analysisScope: GlobalSearchScope

    /**
     * 判断该 PSI 元素是否处于 [analysisScope] 之内,
     * 即是否可被当前 session 分析与构建 symbol。
     */
    fun PsiElement.canBeAnalysed(): Boolean
}

/**
 * 当前 [CaSession] 可分析文件集合的全局搜索作用域,顶层桥接形式。
 */
context(session: CaSession)
val analysisScope: GlobalSearchScope
    get() = with(session) { analysisScope }
