package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CfirExtendSemantics
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.source.text

/**
 * `@Annotation` 修饰的声明必须提供可在编译期构造的 const constructor。
 *
 * 这属于 declaration 层规则：
 * 它不依赖调用点，而是约束“被标记为注解类型的声明自身”。
 */
object CfirAnnotationDeclarationChecker : CfirClassLikeChecker() {
    /** 内置 `Annotation` 注解短名。 */
    private val annotationName = Name.identifier("Annotation")

    /** 检查被 `@Annotation` 标记的 class-like 声明是否拥有 const 构造器。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirClassLikeDeclaration) {
        if (!declaration.hasBuiltInAnnotation(annotationName)) return

        val constructors = declaration.declarations.filterIsInstance<CfirConstructor>()
        val hasConstConstructor = constructors.any { constructor -> constructor.status.isConst }
        if (hasConstConstructor) return

        reporter.reportOn(
            source = declaration.source,
            factory = CfirErrors.ANNOTATION_NO_CONST_INIT,
        )
    }
}

/** 判断 class-like 声明是否携带指定内置注解。 */
private fun CfirClassLikeDeclaration.hasBuiltInAnnotation(annotationName: Name): Boolean {
    return annotations.any { annotation ->
        val annotationClassId = CfirExtendSemantics.run { annotation.typeRef.toClassIdOrNull() }
        annotationClassId?.shortClassName == annotationName ||
                annotation.source?.text?.toString()?.contains("@${annotationName.asString()}") == true
    } || source?.text?.toString()?.contains("@${annotationName.asString()}") == true
}
