package org.cangnova.cangjie.cfir

import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.session.CfirSession
import org.cangnova.cangjie.cfir.session.CfirSessionComponent

inline fun <R> withFileAnalysisExceptionWrapping(file: CfirFile, block: () -> R): R {
    return try {
        block()
    } catch (throwable: Throwable) {
        file.moduleData.session.exceptionHandler.handleExceptionOnFileAnalysis(file, throwable)
    }
}

abstract class CfirExceptionHandler : CfirSessionComponent {
    abstract fun handleExceptionOnElementAnalysis(element: CfirElement, throwable: Throwable): Nothing
    abstract fun handleExceptionOnFileAnalysis(file: CfirFile, throwable: Throwable): Nothing
}
val CfirSession.exceptionHandler: CfirExceptionHandler by CfirSession.sessionComponentAccessor()
