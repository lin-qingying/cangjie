package org.cangnova.cangjie.cfir.declarations

import org.cangnova.cangjie.LanguageVersion
import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.annotations.CangjieAnnotationIdentity
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.CfirExpression
import org.cangnova.cangjie.cfir.expressions.CfirNamedArgumentExpression
import org.cangnova.cangjie.cfir.expressions.cjmpAnnotationLiteralConstant
import org.cangnova.cangjie.cfir.expressions.platformAnnotationDescriptor
import org.cangnova.cangjie.cfir.references.CfirResolvedNamedReference
import org.cangnova.cangjie.name.Name
import java.util.Collections

/** 原生 SaveAnnotations 的语义角色；不依赖 FlatBuffers 的枚举或 builder。 */
enum class CfirAnnotationExportKind {
    DEPRECATED, FROZEN, TEST_REGISTRATION, ANNOTATION, CUSTOM,
    JAVA_MIRROR, JAVA_IMPL, JAVA_HAS_DEFAULT, OBJ_C_MIRROR, OBJ_C_IMPL, FOREIGN_NAME,
}

/** 已选中的注解实参保持源序与显式名称；expression 是完成 SOURCE 图中的来源身份。 */
data class CfirAnnotationExportArgument(val name: Name?, val expression: CfirExpression)

/** 声明的不可变注解写出计划；语法参数、真实表达式与空参数记录分别由准备 owner 裁决。 */
data class CfirAnnotationExportPlan(
    val kind: CfirAnnotationExportKind,
    val identifier: String,
    val arguments: List<CfirAnnotationExportArgument>,
    val target: CfirDeclaration?,
)

/** Attribute/overflow/when 的实参由 parser 拥有，不属于已解析的普通表达式域。 */
fun CfirAnnotationCall.hasParserOwnedExportArguments(): Boolean =
    (annotationIdentity as? CangjieAnnotationIdentity.LanguageBuiltIn)?.kind in setOf(
        BuiltInAnnotationKind.ATTRIBUTE, BuiltInAnnotationKind.NUMERIC_OVERFLOW, BuiltInAnnotationKind.WHEN,
    )

/**
 * 在解析完成、SOURCE 捕获之前准备原生注解角色。Frozen/TestRegistration 固定为空实参；
 * 其它普通注解仅保留 native 支持的字面量，Annotation 的目标表达式保持完整图。
 */
fun CfirDeclaration.prepareAnnotationExportPlans(version: LanguageVersion): List<CfirAnnotationExportPlan> =
    Collections.unmodifiableList(annotations.filterIsInstance<CfirAnnotationCall>().mapNotNull { annotation ->
        val kind = when (val identity = annotation.annotationIdentity) {
            is CangjieAnnotationIdentity.LanguageBuiltIn -> when (identity.kind) {
                BuiltInAnnotationKind.DEPRECATED -> CfirAnnotationExportKind.DEPRECATED
                BuiltInAnnotationKind.FROZEN -> CfirAnnotationExportKind.FROZEN
                BuiltInAnnotationKind.ATTRIBUTE -> CfirAnnotationExportKind.TEST_REGISTRATION.takeIf {
                    annotationInfo?.attributes?.contains("TEST_REGISTER") == true
                }
                BuiltInAnnotationKind.ANNOTATION -> CfirAnnotationExportKind.ANNOTATION.takeIf {
                    version >= LanguageVersion.CANGJIE_1_1_0 && annotation.argumentList.arguments.isNotEmpty()
                }
                else -> null
            }
            is CangjieAnnotationIdentity.PlatformDerived -> if (version < LanguageVersion.CANGJIE_1_1_0) null else
                when (annotation.platformAnnotationDescriptor?.officialKind) {
                    BuiltInAnnotationKind.JAVA_MIRROR -> CfirAnnotationExportKind.JAVA_MIRROR
                    BuiltInAnnotationKind.JAVA_IMPL -> CfirAnnotationExportKind.JAVA_IMPL
                    BuiltInAnnotationKind.JAVA_HAS_DEFAULT -> CfirAnnotationExportKind.JAVA_HAS_DEFAULT
                    BuiltInAnnotationKind.OBJ_C_MIRROR -> CfirAnnotationExportKind.OBJ_C_MIRROR
                    BuiltInAnnotationKind.OBJ_C_IMPL -> CfirAnnotationExportKind.OBJ_C_IMPL
                    BuiltInAnnotationKind.FOREIGN_NAME -> CfirAnnotationExportKind.FOREIGN_NAME
                    else -> null
                }
            is CangjieAnnotationIdentity.Custom -> CfirAnnotationExportKind.CUSTOM.takeIf {
                annotation.isCompileTimeVisible == true || (this as? CfirMemberDeclaration)?.status?.let {
                    it.isCommon || it.isSpecific
                } == true
            }
            else -> null
        } ?: return@mapNotNull null
        val identifier = when (val identity = annotation.annotationIdentity) {
            is CangjieAnnotationIdentity.LanguageBuiltIn -> identity.sourceName
            is CangjieAnnotationIdentity.Custom -> annotation.annotationSourceName ?: identity.classFqName?.shortName()?.asString()
            else -> annotation.annotationSourceName
        }
        val arguments = if (kind == CfirAnnotationExportKind.FROZEN || kind == CfirAnnotationExportKind.TEST_REGISTRATION) emptyList()
        else annotation.argumentList.arguments.mapNotNull { argument ->
            val named = argument as? CfirNamedArgumentExpression
            val value = named?.expression ?: argument
            if (kind != CfirAnnotationExportKind.ANNOTATION && value.cjmpAnnotationLiteralConstant() == null) null
            else CfirAnnotationExportArgument(named?.argumentName, value)
        }
        val target = if (kind == CfirAnnotationExportKind.CUSTOM) {
            val reference = checkNotNull(annotation.calleeReference as? CfirResolvedNamedReference) {
                "A visible custom annotation requires its completed constructor binding"
            }
            check(reference.resolvedSymbol.isBound)
            reference.resolvedSymbol.cfir
        } else null
        CfirAnnotationExportPlan(kind, checkNotNull(identifier) { "An exported annotation has no resolved identifier" },
            Collections.unmodifiableList(arguments.toList()), target)
    })
