package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall

/**
 * `@Annotation` 修饰的声明必须提供可在编译期构造的 const constructor。
 *
 * 这属于 declaration 层规则：
 * 它不依赖调用点，而是约束“被标记为注解类型的声明自身”。
 */
object CfirAnnotationDeclarationChecker : CfirClassLikeChecker() {
    /** 检查被 `@Annotation` 标记的 class-like 声明是否拥有 const 构造器。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        if (!declaration.hasBuiltInAnnotation()) return

        val constructors = declaration.declarations.filterIsInstance<CfirConstructor>()
        val hasConstConstructor = constructors.any { constructor ->
            constructor.status.isConst && !constructor.status.isStatic
        }
        if (hasConstConstructor) return

        // 官方注解类资格检查将诊断锚定类名；注解调用本身不是缺失构造器的声明。
        reporter.reportOn(
            source = declaration.source,
            factory = CfirErrors.ANNOTATION_NO_CONST_INIT,
        )
    }
}

/** 只使用 annotation resolve 发布的官方 kind，不从 PSI 文本或短名推断语义。 */
private fun CfirClassLikeDeclaration.hasBuiltInAnnotation(): Boolean =
    annotations.filterIsInstance<CfirAnnotationCall>().any {
        it.annotationKind == BuiltInAnnotationKind.ANNOTATION
    }
