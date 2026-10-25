package org.cangnova.cangjie.analysis.api.cfir.references

import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.idea.references.CDocReference
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocName
import org.cangnova.cangjie.psi.CjImportAlias
import org.cangnova.cangjie.references.CangJiePsiReferenceProviderContributor

/**
 * 基于 CFIR Analysis API 的 CDoc 名称引用。
 *
 * 对位 Kotlin `KaFirKDocReference`：`CDocReference` 提供引用语义（range / rename / canonicalText /
 * resolvesByNames），本类只负责"这个名字要解析成哪些符号"。
 */
@OptIn(CaImplementationDetail::class)
internal class CaCfirCDocReference(
    /**
     * 承载该引用的 CDoc 名称节点。
     */
    name: CDocName,
) : CDocReference(name), CaCfirReference {

    /**
     * 当前引用使用的 CFIR resolver。
     */
    override val resolver get() = CaCfirReferenceResolver

    /**
     * 计算当前 CDoc 名称引用指向的公开符号集合。
     */
    override fun CaCfirSession.computeSymbols(): Collection<CaSymbol> =
        CfirCDocReferenceResolver.resolveCDocReference(this@CaCfirCDocReference, this)

    /**
     * 解析当前引用对应的 PSI 目标集合。
     */
    override fun resolveTargetElements(): Collection<PsiElement> =
        analyze(element) { getResolvedToPsi(this) }

    /**
     * 判断当前引用是否指向给定 import alias。
     *
     * CDoc 链接不参与 import 解析，这里与简单名引用共用同一判定以消除多重继承歧义。
     */
    override fun isReferenceToImportAlias(alias: CjImportAlias): Boolean =
        super<CaCfirReference>.isReferenceToImportAlias(alias)

    /**
     * CDoc 名称引用 provider。
     */
    class Provider : CangJiePsiReferenceProviderContributor<CDocName> {
        /**
         * provider 处理的 PSI 元素类型。
         */
        override val elementClass: Class<CDocName>
            get() = CDocName::class.java

        /**
         * 为每个 CDoc 名称节点创建一条 CDoc 引用。
         */
        override val referenceProvider: CangJiePsiReferenceProviderContributor.ReferenceProvider<CDocName>
            get() = CangJiePsiReferenceProviderContributor.ReferenceProvider { name ->
                listOf(CaCfirCDocReference(name))
            }
    }
}
