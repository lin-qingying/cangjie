package org.cangnova.cangjie.analysis.api.cfir.references

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.CaImplementationDetail
import org.cangnova.cangjie.analysis.api.CaSession
import org.cangnova.cangjie.analysis.api.analyze
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.low.level.api.cfir.api.getOrBuildCfir
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.classIdOrPrimitiveClassId
import org.cangnova.cangjie.idea.references.CjSimpleReference
import org.cangnova.cangjie.psi.CjBasicType
import org.cangnova.cangjie.psi.CjImportAlias
import org.cangnova.cangjie.psi.CjTypeReference
import org.cangnova.cangjie.psi.psiUtil.parentOfType
import org.cangnova.cangjie.references.CangJiePsiReferenceProviderContributor

/**
 * 仓颉 basic-type PSI 不是 Kotlin 那种 `simple-name` 形状，
 * 但在 IDE 引用模型里它仍然代表一个真实的 class-like type 引用。
 *
 * 因此这里把 `CjBasicType -> resolved CfirTypeRef -> public class-like symbol`
 * 接回现有 `CaCfirReference` 主链，避免 `String` / `Int64` 这类 stdlib/builtins
 * 在 target extraction、文档与跳转上继续掉出统一 reference framework。
 */
@OptIn(CaImplementationDetail::class)
internal class CaCfirBasicTypeReference(
    basicType: CjBasicType,
) : CjSimpleReference<CjBasicType>(basicType), CaCfirReference {
    override val resolver get() = CaCfirReferenceResolver

    override fun isReferenceToImportAlias(alias: CjImportAlias): Boolean = false

    override fun CaCfirSession.computeSymbols(): Collection<CaSymbol> {
        val classId = element.containingTypeReference()
            ?.getOrBuildCfir(resolutionFacade)
            ?.let { it as? CfirResolvedTypeRef }
            ?.coneType
            ?.classIdOrPrimitiveClassId
            ?: return emptyList()

        return listOfNotNull(cfirSymbolBuilder.classifierBuilder.buildClassLikeSymbolByClassId(classId))
    }

    override fun resolveTargetElements(): Collection<PsiElement> {
        return analyze(element) { getResolvedToPsi(this) }
    }

    class Provider : CangJiePsiReferenceProviderContributor<CjBasicType> {
        override val elementClass: Class<CjBasicType>
            get() = CjBasicType::class.java

        override val referenceProvider: CangJiePsiReferenceProviderContributor.ReferenceProvider<CjBasicType>
            get() = CangJiePsiReferenceProviderContributor.ReferenceProvider { basicType ->
                listOf(CaCfirBasicTypeReference(basicType))
            }
    }
}

private fun CjBasicType.containingTypeReference(): CjTypeReference? = parentOfType(withSelf = true)
