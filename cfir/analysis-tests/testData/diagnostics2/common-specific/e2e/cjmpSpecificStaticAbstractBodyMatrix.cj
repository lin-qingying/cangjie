// LANGUAGE_VERSION: 1.1.3
// RUN_PIPELINE_TILL: FRONTEND
// SDK 1.1.3：20261113-cjmp-body-matrix-test-writer/cjc-results.json，common-seed 与 specific-explicit。
// common seed 可正常生成 CHIR/CJO；specific 两段式编译报函数/属性缺体及两项 modifier mismatch。
// 该配对有真实 modifier mismatch，不能称为零额外诊断的配对正例。
// MODULE: common
// CJMP_MODE: COMMON
// FILE: common.cj
package body_matrix
public common abstract class Matrix {
    public common init()
    public common static func fn(): Unit
    public common static prop value: Int64
}

// MODULE: specific()()(common)
// CJMP_MODE: SPECIFIC
// FILE: specific.cj
package body_matrix
public specific abstract class Matrix {
    public specific init() {}
    <!SPECIFIC_HAS_DIFFERENT_MODIFIER!>public specific static abstract <!MISSING_FUNC_BODY!>func fn(): Unit<!><!>
    <!SPECIFIC_HAS_DIFFERENT_MODIFIER!>public specific static abstract <!MISSING_FUNC_BODY!>prop value: Int64<!><!>
}
