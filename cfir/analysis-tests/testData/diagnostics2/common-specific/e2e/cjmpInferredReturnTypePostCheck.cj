// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3: matching inferred body returns are accepted; an inferred String specific return against common Int64
// retains the first-fit pair and reports RETURN_TYPE_INCOMPATIBLE without NOT_MATCHED.
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common func inferredSame() { 1 }
public common func inferredMismatch() { 1 }

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// MAIN_MODULE
// FILE: specific.cj
package cjmp_p

public specific func inferredSame() { 1 }
<!RETURN_TYPE_INCOMPATIBLE!>public specific func inferredMismatch() { "wrong" }<!>
