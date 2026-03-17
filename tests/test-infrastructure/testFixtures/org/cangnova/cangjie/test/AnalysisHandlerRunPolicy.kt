package org.cangnova.cangjie.test

import org.cangnova.cangjie.test.model.AnalysisHandler

fun AnalysisHandler<*>.shouldRun(thereWasAnException: Boolean): Boolean {
    return !(doNotRunIfThereWerePreviousFailures && thereWasAnException)
}
