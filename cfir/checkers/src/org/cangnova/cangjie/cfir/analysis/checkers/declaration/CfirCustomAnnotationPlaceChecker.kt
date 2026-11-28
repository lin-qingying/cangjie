package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirAnonymousFunction
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirFieldVariable
import org.cangnova.cangjie.cfir.declarations.CfirNamedFunction
import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAnnotation
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.session.annotationMetadataRegistryOrNull
import org.cangnova.cangjie.cfir.session.symbolProvider

/**
 * 自定义注解的合法放置检查器。
 *
 * 对齐 C++ DiagKind::sema_annotation_custom_place (TypeCheckAnnotation.cpp:88):
 * 自定义注解(即注解类带 @Annotation 元注解)只能放在非 local 声明上;
 * 局部 function / property / var 不允许带自定义注解。
 */
object CfirCustomAnnotationPlaceChecker : CfirBasicDeclarationChecker() {
    /**
     * 沿统一声明分派检查局部声明，注解身份由类型解析阶段发布。
     */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirDeclaration) {
        val isLocal = when (declaration) {
            is CfirNamedFunction -> declaration.isLocal
            is CfirAnonymousFunction -> declaration.isLocal
            is CfirProperty -> declaration.isLocal
            is CfirFieldVariable -> false // 字段本身属于类型成员,合法
            is CfirVariable -> declaration.isLocal
            else -> return
        }
        if (!isLocal) return

        for (ann in declaration.annotations) {
            if (isCustomAnnotation(ann)) {
                val annotationSource = (ann as? CfirAnnotationCall)?.let { call ->
                    context.session.annotationMetadataRegistryOrNull?.snapshot(call)?.annotationSource
                }
                reporter.reportOn(
                    source = annotationSource ?: ann.source ?: declaration.source,
                    factory = CfirErrors.ANNOTATION_CUSTOM_PLACE,
                )
            }
        }
    }

    /**
     * 使用 resolver 发布的 ClassId 核验真实注解类，避免读取表达式的旧类型槽。
     */
    context(context: CheckerContext)
    private fun isCustomAnnotation(annotation: CfirAnnotation): Boolean {
        val classId = annotation.annotationClassId ?: return false
        val classDecl = context.session.symbolProvider
            .getClassLikeSymbolByClassId(classId)?.cfir
            as? CfirClassLikeDeclaration ?: return false
        return classDecl.hasBuiltinAnnotation(BuiltInAnnotationKind.ANNOTATION)
    }
}
