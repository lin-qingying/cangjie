/*
 * Copyright 2010-2024 cangjie.
 */

package org.cangnova.cangjie.cfir.analysis.checkers

/**
 * - [MppCheckerKind.Common] means this checker runs in the session to which the current declaration belongs.
 * - [MppCheckerKind.Platform] means this checker runs with a platform-specific session in MPP mode.
 */
enum class MppCheckerKind {
    Common,
    Platform,
}

interface CfirCheckerWithMppKind {
    val mppKind: MppCheckerKind
}

