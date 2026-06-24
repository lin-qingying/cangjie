package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.cfir.types.ConeClassLikeType
import org.cangnova.cangjie.name.Name

/**
 * 自定义注解的合法放置检查器。
 *
 * 对齐 C++ DiagKind::sema_annotation_custom_place (TypeCheckAnnotation.cpp:88):
 * 自定义注解(即注解类带 @Annotation 元注解)只能放在非 local 声明上;
 * 局部 function / property / var 不允许带自定义注解。
 */
object CfirCustomAnnotationPlaceChecker : CfirCallableDeclarationChecker() {
    /**
     * 标识自定义注解类的元注解名称。
     */
    private val ANNOTATION = Name.identifier("Annotation")

    /**
     * 检查 callable 声明上的自定义注解是否放置在局部声明上。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirCallableDeclaration) {
        val isLocal = when (declaration) {
            is CfirNamedFunction -> declaration.isLocal
            is CfirAnonymousFunction -> declaration.isLocal
            is CfirProperty -> declaration.isLocal
            is CfirFieldVariable -> false // 字段本身属于类型成员,合法
            else -> return
        }
        if (!isLocal) return

        for (ann in declaration.annotations) {
            if (isCustomAnnotation(ann)) {
                reporter.reportOn(
                    source = ann.source ?: declaration.source,
                    factory = CfirErrors.ANNOTATION_CUSTOM_PLACE,
                )
            }
        }
    }

    /**
     * 判断注解调用的类型是否指向带 `@Annotation` 元注解的自定义注解类。
     */
    context(context: CheckerContext)
    private fun isCustomAnnotation(annotation: CfirAnnotation): Boolean {
        val type = (annotation.typeRef as? CfirResolvedTypeRef)?.coneType as? ConeClassLikeType ?: return false
        val classDecl = context.session.symbolProvider
            .getClassLikeSymbolByClassId(type.classId)?.cfir
            as? CfirClassLikeDeclaration ?: return false
        return classDecl.hasAnnotation(ANNOTATION)
    }
}
