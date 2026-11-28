// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3 probe recorded in docs/cjmp-framework-review-and-fix-plan-20260928.md (F5, version table): both nominal declarations are generic, so they pair; the declaration-level generic check reports the arity difference.
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

public common class Box<T> {
    public common init() {}
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

<!GENERIC_MEMBER_TYPE_ARGUMENT_DIFFERENT!>public specific class Box<T, U> {
    public specific init() {}
}<!>
