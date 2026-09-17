package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.annotations.BuiltInAnnotationKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.CfirClassLikeDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.declarations.annotationTargetFor
import org.cangnova.cangjie.cfir.declarations.annotationInfo
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall
import org.cangnova.cangjie.cfir.expressions.builtInDescriptor
import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.symbols.lazyResolveToPhase
import org.cangnova.cangjie.cfir.declarations.CfirResolvePhase

/**
 * 自定义注解目标检查器。
 *
 * 官方 CHIR `AnnotationChecker` 在注解实例已经解析后，用注解类型的 target 位集
 * 与当前声明的十类目标逐项匹配。CFIR 在同一个 declaration checker seam 完成同一
 * 检查，避免把目标判断分散到不同声明 checker，也不从 annotation 短名回猜注解类型。
 */
object CfirAnnotationTargetChecker : CfirBasicDeclarationChecker() {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirDeclaration) {
        val target = declaration.annotationTargetFor() ?: return
        for (annotation in declaration.annotations.filterIsInstance<CfirAnnotationCall>()) {
            val builtIn = annotation.builtInDescriptor
            if (builtIn != null) {
                // C/CallingConv/FastNative/Frozen 拥有各自的官方错误分类与
                // 作用域顺序，由 CfirCAnnotationChecker 统一负责；这里不能
                // 再生成一般的 ANNOTATION_NOT_APPLICABLE_JFFI。
                if (builtIn.kind in BUILT_INS_WITH_DEDICATED_TARGET_CHECKER) continue
                if (target in builtIn.declarationTargets) continue
                if (builtIn.declarationTargets.isEmpty()) continue

                reporter.reportOn(
                    source = annotation.source ?: declaration.source,
                    factory = CfirErrors.ANNOTATION_NOT_APPLICABLE_JFFI,
                    a = builtIn.sourceName,
                    b = target.description,
                )
                continue
            }

            val annotationClassId = annotation.annotationClassId ?: continue
            val annotationSymbol = context.session.symbolProvider
                .getClassLikeSymbolByClassId(annotationClassId)
                ?: continue
            annotationSymbol.lazyResolveToPhase(CfirResolvePhase.BODY_RESOLVE)
            val annotationType = annotationSymbol.cfir as? CfirClassLikeDeclaration ?: continue
            val info = annotationType.annotationInfo ?: continue
            if (!info.isAnnotation || target in info.annotationTargets) continue

            reporter.reportOn(
                source = annotation.source ?: declaration.source,
                factory = CfirErrors.ANNOTATION_NOT_APPLICABLE_JFFI,
                a = annotation.displayName(),
                b = target.description,
            )
        }
    }

    private fun CfirAnnotationCall.displayName(): String =
        annotationSourceName?.removePrefix("@")?.takeIf(String::isNotBlank)
            ?: annotationClassId?.shortClassName?.asString()
            ?: "<unknown>"

    /** 这些内置注解的非法目标需要保留其专用 CFFI 错误，而不是通用 target 错误。 */
    private val BUILT_INS_WITH_DEDICATED_TARGET_CHECKER = setOf(
        BuiltInAnnotationKind.C,
        BuiltInAnnotationKind.CALLING_CONV,
        BuiltInAnnotationKind.FASTNATIVE,
        BuiltInAnnotationKind.FROZEN,
    )

}
