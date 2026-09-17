package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.expressions.*
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.isIntegerType
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.AbstractCjSourceElement
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement
import org.cangnova.cangjie.source.CjSourceElement
import org.cangnova.cangjie.source.text

/** checker 只能读取 annotation resolver 已确认的 ClassId。 */
internal fun CfirDeclaration.hasAnnotation(annotationClassId: ClassId): Boolean =
    findAnnotations(annotationClassId).isNotEmpty()

internal fun CfirDeclaration.findAnnotations(annotationClassId: ClassId): List<CfirAnnotation> =
    annotations.filter { it.annotationClassId == annotationClassId }

/**
 * 按 resolver 已发布的官方 kind 查找内置注解。
 *
 * 这是语义 checker 的主入口，不会把同名 custom annotation 或限定名 annotation
 * 误认为语言内置注解。
 */
internal fun CfirDeclaration.hasBuiltinAnnotation(kind: BuiltInAnnotationKind): Boolean =
    annotations.any { it.annotationKind == kind }

internal fun CfirDeclaration.findBuiltinAnnotations(kind: BuiltInAnnotationKind): List<CfirAnnotation> =
    annotations.filter { it.annotationKind == kind }

internal fun CfirDeclaration.hasAnyBuiltinAnnotation(vararg kinds: BuiltInAnnotationKind): Boolean =
    kinds.any(::hasBuiltinAnnotation)

/** 同 kind 的 Overflow 拼写必须从 resolver 保留的 descriptor 获取。 */
internal fun CfirAnnotation.shortNameOrNull(): Name? =
    (this as? CfirAnnotationCall)?.builtInDescriptor?.sourceName?.let(Name::identifier)
        ?: annotationClassId?.shortClassName

internal fun CfirAnnotationCall.argumentTextAt(index: Int): String? =
    (argumentView?.entries?.filterNot { it.isDefaultOrigin }?.getOrNull(index)?.constantExpression as? CfirLiteralExpression)?.value?.toString()

internal fun CfirAnnotationCall.argumentCount(): Int =
    argumentView?.entries?.count { !it.isDefaultOrigin && it.argument != null } ?: 0

internal fun CfirAnnotationCall.hasArguments(): Boolean = argumentCount() != 0

internal fun CfirAnnotationCall.hasNamedArgument(name: String): Boolean =
    argumentView?.entries?.any {
        !it.isDefaultOrigin && it.argument != null &&
            (it.explicitName?.asString() == name || it.resolvedParameter?.name?.asString() == name)
    } == true

internal fun CfirAnnotationCall.firstArgumentIsNamed(): Boolean =
    argumentView?.entries?.firstOrNull { !it.isDefaultOrigin }?.explicitName != null

internal fun CfirAnnotationCall.rawNamedArgumentNames(): List<String> =
    argumentView?.entries.orEmpty().filterNot { it.isDefaultOrigin }.mapNotNull { it.explicitName?.asString() }

internal fun CfirAnnotationCall.argumentByName(name: String): CfirExpression? = argumentValue(name)

internal fun CfirAnnotationCall.namedArgumentText(name: String): String? =
    (argumentValue(name) as? CfirLiteralExpression)?.value?.toString()

internal fun CfirAnnotationCall.argumentsAreLiteralLike(): Boolean =
    explicitArgumentExpressions().all { it.isAnnotationLiteralLike() }

/** 返回当前 annotation 已发布的显式参数；view 尚未发布时使用同一 call 的原始 CFIR 参数列表。 */
internal fun CfirAnnotationCall.explicitArgumentExpressions(): List<CfirExpression> =
    argumentView
        ?.entries
        ?.asSequence()
        ?.filter { !it.isDefaultOrigin && it.argument != null }
        ?.map { it.argument!! }
        ?.toList()
        ?.takeIf { it.isNotEmpty() }
        ?: argumentList.arguments

/**
 * 判断 APILevel 是否提供了语法上的首个 level/since 参数。
 *
 * `APILEVEL_MISSING_ARG` 针对缺失参数，而不是针对参数值不是 literal；后者由
 * `ONLY_LITERAL_SUPPORT` 在实际参数表达式上报告。保留这个结构事实可以避免在
 * 参数绑定失败时把一个已写出的旧版位置参数重复诊断为“缺失”。
 */
internal fun CfirAnnotationCall.hasApiLevelSinceArgument(): Boolean {
    val explicitView = argumentView?.entries
        ?.filter { !it.isDefaultOrigin && it.argument != null }
        ?.takeIf { it.isNotEmpty() }
    if (explicitView != null) {
        if (explicitView.any { it.explicitName?.asString() == "since" || it.explicitName?.asString() == "level" }) return true
        return explicitView.firstOrNull()?.explicitName == null
    }

    val explicitArguments = argumentList.arguments
    if (explicitArguments.any {
            val name = (it as? CfirNamedArgumentExpression)?.argumentName?.asString()
            name == "since" || name == "level"
        }) return true
    return explicitArguments.firstOrNull() !is CfirNamedArgumentExpression && explicitArguments.isNotEmpty()
}

internal fun CfirAnnotationCall.firstArgumentIsBooleanLiteralNamed(name: String): Boolean =
    (argumentValue(name) as? CfirLiteralExpression)?.value is Boolean

/**
 * 判断 annotation 参数是否属于平台注解允许的 literal 形态。
 *
 * APILevel 的旧版首个 `level` 参数在官方测试数据中允许已经解析为整数常量的
 * const 引用；syscap 等字符串参数仍然必须是源码字面量。这个例外只对整数 const
 * 变量开放，不能把任意 const 引用或普通表达式当成 annotation literal。
 */
internal fun CfirExpression.isAnnotationLiteralLike(): Boolean = when (this) {
    is CfirNamedArgumentExpression -> expression.isAnnotationLiteralLike()
    is CfirLiteralExpression -> true
    is CfirArrayLiteral -> elements.all { it.isAnnotationLiteralLike() }
    is CfirQualifiedAccessExpression -> {
        val reference = calleeReference as? CfirResolvedNamedReference ?: return false
        val variable = reference.resolvedSymbol.cfir as? CfirVariable ?: return false
        val type = variable.returnTypeRef as? CfirResolvedTypeRef ?: return false
        variable.status.isConst && type.coneType.isIntegerType
    }
    else -> false
}

/** 取得 annotation literal 诊断应覆盖的真实参数 token，而不是其前导空白。 */
internal fun CfirExpression.annotationLiteralDiagnosticSource(): AbstractCjSourceElement? = when (this) {
    is CfirNamedArgumentExpression -> expression.annotationLiteralDiagnosticSource()
    is CfirNamedAccessExpression -> {
        val baseSource = calleeReference.source ?: source
        val reference = calleeReference as? org.cangnova.cangjie.cfir.references.CfirNamedReference
        val nameLength = reference?.name?.asString()?.length ?: 0
        if (baseSource != null && nameLength > 0 && baseSource.endOffset - baseSource.startOffset >= nameLength) {
            CjOffsetsOnlySourceElement(baseSource.endOffset - nameLength, baseSource.endOffset)
        } else {
            baseSource.trimmedAnnotationArgumentSource()
        }
    }
    is CfirQualifiedAccessExpression -> (calleeReference.source ?: source).trimmedAnnotationArgumentSource()
    else -> source.trimmedAnnotationArgumentSource()
}

/**
 * 在宿主 annotation source 中重新定位参数 token。
 *
 * annotation 表达式可能来自局部重解析，其节点 offset 会包含重解析片段的前导空白；
 * 宿主 annotation source 仍拥有完整文件坐标，因此只用它恢复范围，不参与语义判定。
 */
internal fun CfirExpression.annotationLiteralDiagnosticSource(
    annotationSource: CjSourceElement?,
): AbstractCjSourceElement? {
    val argumentExpression = (this as? CfirNamedArgumentExpression)?.expression ?: this
    val namedAccess = argumentExpression as? CfirNamedAccessExpression
    val name = (namedAccess?.calleeReference as? org.cangnova.cangjie.cfir.references.CfirNamedReference)
        ?.name
        ?.asString()
    val container = annotationSource ?: return annotationLiteralDiagnosticSource()
    val containerText = container.text?.toString()
    if (!name.isNullOrEmpty() && containerText != null) {
        val localStart = (argumentExpression.source?.startOffset ?: container.startOffset) - container.startOffset
        val tokenStart = containerText.indexOf(name, localStart.coerceAtLeast(0)).takeIf { it >= 0 }
            ?: containerText.lastIndexOf(name)
        if (tokenStart >= 0) {
            return CjOffsetsOnlySourceElement(
                container.startOffset + tokenStart,
                container.startOffset + tokenStart + name.length,
            )
        }
    }
    return annotationLiteralDiagnosticSource()
}

/** 收窄 CFIR source 中可能携带的参数前后空白，保持诊断覆盖实际 token。 */
private fun AbstractCjSourceElement?.trimmedAnnotationArgumentSource(): AbstractCjSourceElement? {
    this ?: return null
    val sourceText = (this as? CjSourceElement)?.text?.toString() ?: return this
    val first = sourceText.indexOfFirst { !it.isWhitespace() }
    val last = sourceText.indexOfLast { !it.isWhitespace() }
    if (first < 0 || last < first) return this
    return CjOffsetsOnlySourceElement(startOffset + first, startOffset + last + 1)
}

internal fun CfirExpression.literalStringOrNull(): String? =
    ((this as? CfirNamedArgumentExpression)?.expression ?: this).let { it as? CfirLiteralExpression }?.value as? String

internal fun CfirAnnotationCall.booleanArgument(name: String): Boolean? =
    (argumentValue(name) as? CfirLiteralExpression)?.value as? Boolean
