/*
 * Copyright 2010-2024 cangjie.
 */

package org.cangnova.cangjie.cfir.analysis.checkers

/**
 * - [CheckerDispatchKind.Common] means this checker runs in the session to which the current declaration belongs.
 * - [CheckerDispatchKind.Platform] means this checker runs with an alternate dispatch session.
 */
enum class CheckerDispatchKind {
    Common,
    Platform,
}

interface CfirCheckerWithDispatchKind {
    val dispatchKind: CheckerDispatchKind
}


