package org.cangnova.cangjie.cfir.analysis.checkers.type

import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors
import org.cangnova.cangjie.cfir.declarations.declarationAvailabilityProvider
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.resolve.toClassLikeSymbol
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef
import org.cangnova.cangjie.source.CjOffsetsOnlySourceElement

/**
 * 对已正常解析的类型目标执行 `@!Hide[isChecked: true]` 后置可用性检查。
 *
 * 该 checker 只上报官方 undeclared 形态对应的 [CfirErrors.UNRESOLVED_REFERENCE]，
 * 不把 resolved type 改写为 error type，因而不会制造上界、继承或调用推断级联诊断。
 */
object CfirHideResolvedTypeRefChecker : CfirResolvedTypeRefChecker() {
    /** 检查最终 class-like symbol 在当前文件包中是否因 Hide 不可引用。 */
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(typeRef: CfirResolvedTypeRef) {
        val source = typeRef.source ?: return
        val file = context.containingFileSymbol?.takeIf { it.isBound }?.cfir ?: return
        val symbol = typeRef.coneType.toClassLikeSymbol(context.session)?.takeIf { it.isBound } ?: return
        val availability = context.session.declarationAvailabilityProvider
        val useSitePackage = file.packageDirective.packageFqName
        if (availability.hideUnavailabilityAt(symbol, useSitePackage) == null) return

        reporter.reportOn(
            source = CjOffsetsOnlySourceElement(source.startOffset, source.endOffset),
            factory = CfirErrors.UNRESOLVED_REFERENCE,
            a = symbol.classId.shortClassName.asString(),
            b = null,
        )
    }
}
