

package org.cangnova.cangjie.analysis.low.level.api.cfir.stubBased.deserialization

import org.cangnova.cangjie.cfir.diagnostics.ConeSimpleDiagnostic
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticKind
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.builder.buildAnnotationCall
import org.cangnova.cangjie.cfir.expressions.builder.buildArgumentList
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirLiteralKind
import org.cangnova.cangjie.cfir.expressions.builder.buildArrayLiteral
import org.cangnova.cangjie.cfir.expressions.builder.buildErrorExpression
import org.cangnova.cangjie.cfir.expressions.builder.buildLiteralExpression
import org.cangnova.cangjie.cfir.references.builder.buildErrorNamedReference
import org.cangnova.cangjie.cfir.references.builder.buildResolvedNamedReference
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.CfirBasedSymbol
import org.cangnova.cangjie.cfir.symbols.constructClassType
import org.cangnova.cangjie.cfir.symbols.toLookupTag
import org.cangnova.cangjie.cfir.types.builder.buildResolvedTypeRef
import org.cangnova.cangjie.descriptors.annotations.AnnotationUseSiteTarget
import org.cangnova.cangjie.lexer.CjTokens
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.psi.CjAnnotated
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.psi.CjCollectionLiteralExpression
import org.cangnova.cangjie.psi.CjConstantExpression
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjProperty
import org.cangnova.cangjie.psi.CjStringTemplateExpression
import org.cangnova.cangjie.psi.CjTypeElement
import org.cangnova.cangjie.psi.CjUserType
import org.cangnova.cangjie.source.CjRealPsiSourceElement
import org.cangnova.cangjie.utils.exceptions.errorWithAttachment
import org.cangnova.cangjie.utils.exceptions.requireWithAttachment
import org.cangnova.cangjie.utils.exceptions.withPsiEntry

/**
 * 从 compiled PSI/stub 中反序列化 CFIR 注解和注解常量参数。
 */
internal class StubBasedAnnotationDeserializer(private val session: CfirSession) {
    companion object {
        fun getAnnotationClassId(annotation: CjAnnotation): ClassId {
            val userType = annotation.typeReference?.typeElement
            requireWithAttachment(
                userType is CjUserType,
                { "${CjTypeElement::class.simpleName} should be ${CjUserType::class.simpleName}" },
            ) {
                withPsiEntry("annotationEntry", annotation)
            }

            return userType.classId()
        }

        val TYPE_ANNOTATIONS_FILTER: (AnnotationUseSiteTarget?) -> Boolean = { target ->
            target == null
        }
    }

    /**
     * 仓颉主干目前只承载“无 use-site target”的注解声明。
     * low-level 反序列化这里直接复用 PSI 上真实存在的 `CjAnnotation` 形态，不再维持 Kotlin 的 annotation-stub 常量映射模型。
     */
    fun loadAnnotations(
        annotated: CjAnnotated,
        containingDeclarationSymbol: CfirBasedSymbol<*>? = null,
        useSiteTargetFilter: ((AnnotationUseSiteTarget?) -> Boolean)? = null,
    ): List<CfirAnnotation> {
        if (useSiteTargetFilter?.invoke(null) == false) return emptyList()

        val annotations = annotated.annotationEntries
        if (annotations.isEmpty()) return emptyList()

        val owner = containingDeclarationSymbol ?: errorWithAttachment(
            "Stub-based annotation deserialization requires containing declaration symbol",
        ) {
            withPsiEntry("annotated", annotated)
        }

        return annotations.map { deserializeAnnotation(it, owner) }
    }

    /**
     * 本地主干 `CjPropertyStub` 当前不保存常量初始化器，compiled PSI 也没有可复用的 initializer 入口。
     * 在补齐真正的 property-const stub 基础设施前，这里只能返回 `null`，避免继续依赖 Kotlin 的 `constantInitializer` 漂移接口。
     */
    fun loadConstant(property: CjProperty, isUnsigned: Boolean): CfirExpression? {
        if (!property.hasModifier(CjTokens.CONST_KEYWORD)) return null
        return property.initializer?.let(::deserializeExpression)
    }

    /**
     * 将单个 PSI 注解反序列化为 [CfirAnnotation]。
     */
    private fun deserializeAnnotation(annotation: CjAnnotation, owner: CfirBasedSymbol<*>): CfirAnnotation {
        val source = CjRealPsiSourceElement(annotation)
        val classId = getAnnotationClassId(annotation)
        val typeRef = buildResolvedTypeRef {
            this.source = source
            coneType = classId.toLookupTag().constructClassType()
        }
        val arguments = annotation.valueArguments.mapNotNull { argument ->
            argument.getArgumentExpression()?.let(::deserializeExpression)
        }
        val classSymbol = session.symbolProvider.getClassLikeSymbolByClassId(classId)

        return buildAnnotationCall {
            this.source = source
            this.typeRef = typeRef
            coneTypeOrNull = typeRef.coneType
            this.arguments += arguments
            argumentList = buildArgumentList {
                this.source = source
                this.arguments += arguments
            }
            calleeReference = if (classSymbol != null) {
                buildResolvedNamedReference {
                    this.source = source
                    name = classId.shortClassName
                    resolvedSymbol = classSymbol
                }
            } else {
                buildErrorNamedReference {
                    this.source = source
                    name = classId.shortClassName
                    diagnostic = ConeSimpleDiagnostic(
                        "Unresolved annotation class: ${classId.asString()}",
                        DiagnosticKind.DeserializationError,
                    )
                }
            }
            containingDeclarationSymbol = owner
        }
    }

    /**
     * 反序列化注解实参表达式。
     */
    private fun deserializeExpression(expression: CjExpression): CfirExpression {
        return when (expression) {
            is CjConstantExpression -> deserializeConstantExpression(expression)
            is CjStringTemplateExpression -> deserializeStringTemplate(expression)
            is CjCollectionLiteralExpression -> buildArrayLiteral {
                source = CjRealPsiSourceElement(expression)
                elements += expression.innerExpressions.map(::deserializeExpression)
            }
            else -> buildUnsupportedExpression(expression)
        }
    }

    /**
     * 反序列化常量表达式为 CFIR literal。
     */
    private fun deserializeConstantExpression(expression: CjConstantExpression): CfirExpression {
        val literalKind = when (expression.node.elementType) {
            org.cangnova.cangjie.psi.CjNodeTypes.INTEGER_CONSTANT -> CfirLiteralKind.INT
            org.cangnova.cangjie.psi.CjNodeTypes.FLOAT_CONSTANT -> CfirLiteralKind.FLOAT
            org.cangnova.cangjie.psi.CjNodeTypes.RUNE_CONSTANT -> CfirLiteralKind.RUNE
            org.cangnova.cangjie.psi.CjNodeTypes.BOOLEAN_CONSTANT -> CfirLiteralKind.BOOLEAN
            org.cangnova.cangjie.psi.CjNodeTypes.UNIT_CONSTANT -> CfirLiteralKind.UNIT
            else -> CfirLiteralKind.STRING
        }

        val literalValue = when (literalKind) {
            CfirLiteralKind.BOOLEAN -> expression.text == "true"
            CfirLiteralKind.UNIT -> null
            else -> expression.text
        }

        return buildLiteralExpression {
            source = CjRealPsiSourceElement(expression)
            kind = literalKind
            value = literalValue
        }
    }

    /**
     * 反序列化无插值字符串模板。
     */
    private fun deserializeStringTemplate(expression: CjStringTemplateExpression): CfirExpression {
        if (expression.hasInterpolation()) {
            return buildUnsupportedExpression(expression)
        }

        return buildLiteralExpression {
            source = CjRealPsiSourceElement(expression)
            kind = CfirLiteralKind.STRING
            value = expression.stringContent
        }
    }

    /**
     * 为当前 stub 反序列化不支持的表达式构造错误表达式。
     */
    private fun buildUnsupportedExpression(expression: CjExpression): CfirExpression {
        return buildErrorExpression {
            source = CjRealPsiSourceElement(expression)
            diagnostic = ConeSimpleDiagnostic(
                "Unsupported annotation argument expression: ${expression::class.simpleName}",
                DiagnosticKind.Other,
            )
        }
    }
}
