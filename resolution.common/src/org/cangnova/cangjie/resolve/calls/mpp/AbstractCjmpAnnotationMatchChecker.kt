package org.cangnova.cangjie.resolve.calls.mpp

import org.cangnova.cangjie.annotations.BuiltInAnnotationKind

/** 注解的解析身份及其原始前端载体；相同身份的重复注解仍按出现次数比较。 */
data class CjmpAnnotationMatch<out Key : Any, out Annotation>(
    val key: Key,
    val annotation: Annotation,
)

/** CJO declaration 属性或 class metadata 所表示的注解及其可用的具体注解节点。 */
data class CjmpAnnotationAttribute<out Annotation>(
    val kind: BuiltInAnnotationKind,
    val annotation: Annotation?,
)

/** 注解比较所需的只读事实，独立于声明类型、修饰符和超类型兼容性检查。 */
interface CjmpAnnotationMatchingContext<
    D,
    AnnotationKey : Any,
    Annotation,
> where D : CjmpDeclarationSymbolMarker {
    fun deprecatedAnnotation(declaration: D): Annotation?

    fun annotations(declaration: D): List<CjmpAnnotationMatch<AnnotationKey, Annotation>>

    /** 官方 CJO 以声明属性或 class metadata 表示、而非写入普通注解表的注解身份。 */
    fun nonSerializedAnnotationAttributes(declaration: D): List<CjmpAnnotationAttribute<Annotation>>

    /** 比较显式注解实参；前端按仓颉与序列化协议提供准确的值相等判据。 */
    fun areAnnotationArgumentsEqual(specific: Annotation, common: Annotation): Boolean
}

/** 注解不匹配所在的声明侧；两侧各自的未配对项都必须保留。 */
enum class CjmpAnnotationMismatchSide {
    COMMON,
    SPECIFIC,
}

/** 注解检查独立于声明类型、修饰符和超类型兼容性。 */
sealed interface CjmpAnnotationMatchingIssue<out Annotation> {
    data class DeprecatedAnnotation<out Annotation>(val annotation: Annotation) : CjmpAnnotationMatchingIssue<Annotation>

    data class AnnotationMismatch<out Annotation>(
        val annotation: Annotation,
        val side: CjmpAnnotationMismatchSide,
    ) : CjmpAnnotationMatchingIssue<Annotation>

    data class AnnotationAttributeMismatch<out Annotation>(
        val kind: BuiltInAnnotationKind,
        val annotation: Annotation?,
    ) : CjmpAnnotationMatchingIssue<Annotation>
}

/**
 * 已匹配 common/specific 声明的注解兼容性检查器。
 *
 * 注解匹配拥有独立的声明级检查器，与返回类型、修饰符和超类型兼容性检查分层。仓颉官方规则要求
 * 双向比较，并按注解身份、显式实参和重复次数做一对一匹配；这与 Kotlin 仅遍历 expect 注解的
 * 方向规则不同，因此在共享框架中保留双方未匹配项。
 */
object AbstractCjmpAnnotationMatchChecker {
    /** 返回注解身份、实参和非序列化属性位的差异，调用方负责映射到前端诊断。 */
    fun <D, AnnotationKey : Any, Annotation> checkAnnotations(
        specific: D,
        common: D,
        context: CjmpAnnotationMatchingContext<D, AnnotationKey, Annotation>,
    ): List<CjmpAnnotationMatchingIssue<Annotation>>
        where D : CjmpDeclarationSymbolMarker {
        val specificAnnotations = context.annotations(specific)
        val unmatchedSpecific = specificAnnotations.toMutableList()
        val issues = mutableListOf<CjmpAnnotationMatchingIssue<Annotation>>()

        context.deprecatedAnnotation(specific)?.let { annotation ->
            issues += CjmpAnnotationMatchingIssue.DeprecatedAnnotation(annotation)
        }

        // 官方先比较 CJO declaration attribute，再执行普通 annotation vector 的双向比较。
        val commonAttributes = context.nonSerializedAnnotationAttributes(common)
        val specificAttributes = context.nonSerializedAnnotationAttributes(specific)
        val specificAttributeKinds = specificAttributes.mapTo(hashSetOf()) { it.kind }
        commonAttributes.forEach { attribute ->
            if (attribute.kind !in specificAttributeKinds) {
                val annotation = specificAttributes.firstOrNull { it.kind == attribute.kind }?.annotation
                issues += CjmpAnnotationMatchingIssue.AnnotationAttributeMismatch(
                    attribute.kind,
                    annotation,
                )
            }
        }
        val commonAttributeKinds = commonAttributes.mapTo(hashSetOf()) { it.kind }
        specificAttributes.forEach { attribute ->
            if (attribute.kind !in commonAttributeKinds) {
                issues += CjmpAnnotationMatchingIssue.AnnotationAttributeMismatch(
                    attribute.kind,
                    attribute.annotation,
                )
            }
        }

        // 官方 MatchCJMPDeclAnnotations 先按 common 注解顺序查找 specific 对应项。
        for (commonAnnotation in context.annotations(common)) {
            val specificIndex = unmatchedSpecific.indexOfFirst { specificAnnotation ->
                commonAnnotation.key == specificAnnotation.key &&
                        context.areAnnotationArgumentsEqual(specificAnnotation.annotation, commonAnnotation.annotation)
            }
            if (specificIndex < 0) {
                issues += CjmpAnnotationMatchingIssue.AnnotationMismatch(commonAnnotation.annotation, CjmpAnnotationMismatchSide.COMMON)
            } else {
                unmatchedSpecific.removeAt(specificIndex)
            }
        }

        // 再按 specific 顺序报告 specific-only 注解，保持官方双向 pass 的诊断顺序。
        unmatchedSpecific.forEach { specificAnnotation ->
            issues += CjmpAnnotationMatchingIssue.AnnotationMismatch(
                specificAnnotation.annotation,
                CjmpAnnotationMismatchSide.SPECIFIC,
            )
        }

        return issues
    }
}
