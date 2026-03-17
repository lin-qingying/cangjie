package org.cangnova.cangjie.cfir.analysis.checkers.declaration

import org.cangnova.cangjie.cfir.analysis.checkers.CfirTypeCheckUtils
import org.cangnova.cangjie.cfir.analysis.checkers.CheckerDispatchKind
import org.cangnova.cangjie.cfir.analysis.checkers.context.CheckerContext
import org.cangnova.cangjie.cfir.analysis.diagnostics.CfirErrors

import org.cangnova.cangjie.cfir.declarations.CfirProperty
import org.cangnova.cangjie.cfir.diagnostics.DiagnosticReporter
import org.cangnova.cangjie.cfir.diagnostics.reportOn
import org.cangnova.cangjie.cfir.source.AbstractCjSourceElement
import org.cangnova.cangjie.cfir.types.CfirResolvedTypeRef

/**
 * 灞炴€у垵濮嬪寲绫诲瀷妫€鏌ュ櫒銆? *
 * 妫€鏌ョ被/缁撴瀯浣撴垚鍛樺睘鎬?`var p: T = expr` 涓?`expr` 鐨勭被鍨嬫槸鍚︿负 `T` 鐨勫瓙绫诲瀷銆? */
object CfirPropertyInitializerTypeMismatchChecker : CfirPropertyChecker(CheckerDispatchKind.Common) {
    context(context: CheckerContext, reporter: DiagnosticReporter)
    override fun check(declaration: CfirProperty) {
        val source = declaration.source as? AbstractCjSourceElement ?: return
        val expectedTypeRef = declaration.returnTypeRef as? CfirResolvedTypeRef ?: return
        val actualType = declaration.initializer?.coneTypeOrNull ?: return
        val expectedType = expectedTypeRef.coneType
        if (!CfirTypeCheckUtils.isSubtypeOf(actualType, expectedType)) {
            reporter.reportOn(
                source, CfirErrors.TYPE_MISMATCH,
                expectedType,
                actualType,
                false,
            )
        }
    }
}

