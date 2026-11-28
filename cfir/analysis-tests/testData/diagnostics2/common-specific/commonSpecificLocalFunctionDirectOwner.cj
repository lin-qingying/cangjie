// LANGUAGE_VERSION: 1.1.3
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// SDK 1.1.3：20261113-cjmp-body-matrix-test-writer/cjc-results.json，common-local，零诊断。
// local 是 host 的局部声明，不是外层 common abstract class 的直接成员。
package body_matrix
public common abstract class LocalOwner {
    public common init()
    public func host(): Unit {
        func local(): Unit {}
    }
}
