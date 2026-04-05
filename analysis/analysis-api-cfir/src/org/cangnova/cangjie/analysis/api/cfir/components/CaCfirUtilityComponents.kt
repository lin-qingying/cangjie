package org.cangnova.cangjie.analysis.api.cfir.components

import com.intellij.psi.PsiElement
import org.cangnova.cangjie.analysis.api.cfir.CaCfirSession
import org.cangnova.cangjie.analysis.api.components.CaCInteropComponent
import org.cangnova.cangjie.analysis.api.components.CaDataFlowProvider
import org.cangnova.cangjie.analysis.api.components.CaDocProvider
import org.cangnova.cangjie.analysis.api.components.CaEvaluator
import org.cangnova.cangjie.analysis.api.components.CaExpressionInformationProvider
import org.cangnova.cangjie.analysis.api.components.CaImportOptimizer
import org.cangnova.cangjie.analysis.api.components.CaOriginalPsiProvider
import org.cangnova.cangjie.analysis.api.components.CaReferenceShortener
import org.cangnova.cangjie.analysis.api.components.CaRenderer
import org.cangnova.cangjie.analysis.api.components.CaSourceProvider
import org.cangnova.cangjie.analysis.api.components.CaSymbolInformationProvider
import org.cangnova.cangjie.analysis.api.components.CaSymbolProvider
import org.cangnova.cangjie.analysis.api.components.CaVisibilityChecker
import org.cangnova.cangjie.analysis.api.dataFlow.CaDataFlowInfo
import org.cangnova.cangjie.analysis.api.evaluation.CaCompileTimeValue
import org.cangnova.cangjie.analysis.api.imports.CaImportOptimizationPlan
import org.cangnova.cangjie.analysis.api.imports.CaReferenceShorteningPlan
import org.cangnova.cangjie.analysis.api.interop.CaInteropInfo
import org.cangnova.cangjie.analysis.api.lifetime.withValidityAssertion
import org.cangnova.cangjie.analysis.api.symbols.CaCallableSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaDeclarationSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaFileSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaPackageSymbol
import org.cangnova.cangjie.analysis.api.symbols.CaSymbol
import org.cangnova.cangjie.analysis.api.symbols.pointers.CaSymbolPointer
import org.cangnova.cangjie.analysis.api.types.CaType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile

/**
 * CFIR 符号与工具链组件集合。
 *
 * 这里承载的是 Analysis API 对外暴露的“可消费工具能力”，例如：
 * 符号查询、源码导航、文档、渲染、编译期求值、导入优化和可见性判断。
 */
internal class CaCfirSymbolProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaSymbolProvider, CaCfirSessionComponent {
    override fun CjFile.fileSymbol(): CaFileSymbol = withValidityAssertion {
        analysisSession.createFileSymbol(this@fileSymbol)
    }

    override fun getPackageSymbol(fqName: FqName): CaPackageSymbol? = withValidityAssertion {
        analysisSession.getPackagePublicSymbol(fqName)
    }

    override fun getClassLikeSymbol(classId: ClassId) = withValidityAssertion {
        analysisSession.getClassLikePublicSymbol(classId)
    }

    override fun getTopLevelClassLikeSymbols(packageFqName: FqName, name: Name) = withValidityAssertion {
        analysisSession.getOrCreateTopLevelPublicSymbols(packageFqName, name).classLikeSymbols
    }

    override fun getTopLevelCallableSymbols(packageFqName: FqName, name: Name) = withValidityAssertion {
        analysisSession.getOrCreateTopLevelPublicSymbols(packageFqName, name).callableSymbols
    }
}

/**
 * C 互操作信息入口。
 */
internal class CaCfirCInteropComponent(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaCInteropComponent {
    override fun CjElement.getInteropInfo(): CaInteropInfo? = withValidityAssertion {
        analysisSession.getInteropInfo(this@getInteropInfo)
    }

    override fun CaSymbol.getInteropInfo(): CaInteropInfo? = withValidityAssertion {
        analysisSession.getInteropInfo(this@getInteropInfo)
    }
}

/**
 * 符号指针与恢复入口。
 */
internal class CaCfirSymbolInformationProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaSymbolInformationProvider {
    override fun CaSymbol.createPointer(): CaSymbolPointer<CaSymbol> = withValidityAssertion {
        @Suppress("UNCHECKED_CAST")
        CaCfirSymbolPointerDelegate<CaSymbol>(createRestoreKey())
    }
}

/**
 * 表达式结构与常量性质入口。
 */
internal class CaCfirExpressionInformationProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaExpressionInformationProvider {
    override val CjExpression.isStatementLike: Boolean
        get() = withValidityAssertion {
            this@isStatementLike.isStatementLikeExpression()
        }

    override val CjExpression.isCompileTimeConstant: Boolean
        get() = withValidityAssertion {
            analysisSession.evaluateCompileTimeValue(this@isCompileTimeConstant) != null
        }
}

/**
 * 编译期求值入口。
 */
internal class CaCfirEvaluator(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaEvaluator {
    override fun CjExpression.evaluate(): CaCompileTimeValue? = withValidityAssertion {
        analysisSession.evaluateCompileTimeValue(this@evaluate)
    }
}

/**
 * 引用缩短规划入口。
 */
internal class CaCfirReferenceShortener(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaReferenceShortener {
    override fun CjFile.collectReferenceShorteningPlan(): CaReferenceShorteningPlan = withValidityAssertion {
        analysisSession.collectReferenceShorteningPlan(this@collectReferenceShorteningPlan)
    }
}

/**
 * 导入优化规划入口。
 */
internal class CaCfirImportOptimizer(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaImportOptimizer {
    override fun CjFile.collectImportOptimizationPlan(): CaImportOptimizationPlan = withValidityAssertion {
        analysisSession.collectImportOptimizationPlan(this@collectImportOptimizationPlan)
    }
}

/**
 * 统一文本渲染入口。
 */
internal class CaCfirRenderer(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaRenderer {
    override fun CaSymbol.render(): String = withValidityAssertion {
        when (this@render) {
            is CaPackageSymbol -> fqName.asString()
            is CaFileSymbol -> "${packageFqName.asString()}/${file.name}"
            is CaCallableSymbol -> renderCallableSymbol(this@render)
            is org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol -> renderClassLikeSymbol(this@render)
            else -> name ?: this@render::class.simpleName.orEmpty()
        }
    }

    override fun CaType.render(): String = withValidityAssertion {
        presentation
    }

    /**
     * 统一渲染公开 callable 符号。
     */
    private fun renderCallableSymbol(symbol: CaCallableSymbol): String {
        val signature = with(analysisSession) { symbol.signature }
        val annotationPrefix = renderAnnotations(with(analysisSession) { symbol.annotations })
        if (signature == null) {
            val fallbackName = symbol.callableId?.toString() ?: (symbol.name ?: "<anonymous callable>")
            return annotationPrefix + fallbackName
        }

        val typeParameterText = signature.typeParameters
            .takeIf(List<Name>::isNotEmpty)
            ?.joinToString(prefix = "<", postfix = ">") { it.asString() }
            .orEmpty()
        val valueParameterText = signature.valueParameters.joinToString(prefix = "(", postfix = ")") { parameter ->
            buildString {
                val parameterAnnotations = renderAnnotations(parameter.annotations)
                append(parameterAnnotations)
                append(parameter.name?.asString() ?: "_")
                parameter.typeText?.let { typeText ->
                    append(": ")
                    append(typeText)
                }
            }
        }
        val returnTypeText = signature.returnTypeText?.let { renderedType -> ": $renderedType" }.orEmpty()
        val declarationName = signature.declarationName?.asString()
            ?: symbol.callableId?.toString()
            ?: symbol.name
            ?: "<anonymous callable>"
        return annotationPrefix + declarationName + typeParameterText + valueParameterText + returnTypeText
    }

    /**
     * 当前公开 API 尚未暴露完整 class-like kind 模型，这里稳定输出注解前缀加 ClassId。
     */
    private fun renderClassLikeSymbol(symbol: org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol): String {
        val annotationPrefix = renderAnnotations(with(analysisSession) { symbol.annotations })
        return annotationPrefix + symbol.classId.asString()
    }

    private fun renderAnnotations(annotations: List<org.cangnova.cangjie.analysis.api.annotations.CaAnnotation>): String {
        if (annotations.isEmpty()) return ""
        return annotations.joinToString(separator = " ", postfix = " ") { annotation ->
            annotation.renderedText
        }
    }
}

/**
 * 当前 session 视角下的可见性判定。
 */
internal class CaCfirVisibilityChecker(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaVisibilityChecker, CaCfirSessionComponent {
    override fun CaSymbol.isVisible(): Boolean = withValidityAssertion {
        when (this@isVisible) {
            is CaPackageSymbol -> analysisSession.hasVisiblePackage(fqName)
            is CaCfirFileSymbolImpl -> analysisSession.lookupFileSymbol(file) != null
            is org.cangnova.cangjie.analysis.api.symbols.CaClassLikeSymbol -> {
                val restoredSymbol = when (this@isVisible) {
                    is CaCfirClassLikeSymbolImpl -> analysisSession.getClassLikePublicSymbol(classId)
                    else -> null
                }
                restoredSymbol === this@isVisible
            }

            is CaCallableSymbol -> {
                val restoredSymbol = when (this@isVisible) {
                    is CaCfirCallableSymbolImpl -> callableId?.let(analysisSession::restoreCallablePublicSymbol)
                    else -> null
                }
                restoredSymbol === this@isVisible
            }

            else -> false
        }
    }
}

/**
 * 原始 PSI 定位入口。
 */
internal class CaCfirOriginalPsiProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaOriginalPsiProvider {
    override fun CaSymbol.getOriginalPsi(): PsiElement? = withValidityAssertion {
        when (this@getOriginalPsi) {
            is CaFileSymbol -> file
            is CaDeclarationSymbol -> psi
            else -> null
        }
    }
}

/**
 * 数据流快照入口。
 */
internal class CaCfirDataFlowProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaDataFlowProvider {
    override fun CjExpression.getDataFlowInfo(): CaDataFlowInfo = withValidityAssertion {
        analysisSession.getDataFlowInfo(this@getDataFlowInfo)
    }
}

/**
 * 符号到源码文件的稳定导航入口。
 */
internal class CaCfirSourceProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaSourceProvider {
    override fun CaSymbol.getContainingFile(): CjFile? = withValidityAssertion {
        when (this@getContainingFile) {
            is CaCfirBackedSymbol<*> -> analysisSession.lookupContainingFile(backingSymbol)
            else -> null
        }
    }
}

/**
 * 文档渲染入口。
 */
internal class CaCfirDocProvider(
    override val analysisSessionProvider: () -> CaCfirSession,
) : CaBaseSessionComponent<CaCfirSession>(), CaDocProvider {
    override fun CaSymbol.documentation(): String? = withValidityAssertion {
        analysisSession.renderDocumentation(this@documentation)
    }
}
