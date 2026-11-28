// LANGUAGE_VERSION: 1.1.3
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// SDK 1.1.3：20261113-cjmp-body-matrix-test-writer/cjc-results.json，common-default，零诊断。
// 正常 common 函数体与 getter 由真实源码形成 COMMON_WITH_DEFAULT，不手工改写状态。
package body_matrix_default
public common abstract class DefaultOwner {
    public common init()
    public common func fn(): Unit {}
    public common prop value: Unit { get() {} }
}
