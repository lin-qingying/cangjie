// LANGUAGE_VERSION: 1.1.0
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3 probe: .workbuddy/tmp/cjmp_probe113/generic_nominal_arity; different nominal generic arities do not merge.
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package cjmp_p

<!NOT_MATCHED!>public common class Box<T> {
    <!NOT_MATCHED!>public common init()<!>
}<!>

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package cjmp_p

<!NOT_MATCHED!>public specific class Box {
    <!NOT_MATCHED!>public specific init() {}<!>
}<!>
