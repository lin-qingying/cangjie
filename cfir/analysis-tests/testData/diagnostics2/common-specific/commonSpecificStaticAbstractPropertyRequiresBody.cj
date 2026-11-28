// LANGUAGE_VERSION: 1.1.3
// CJMP_MODE: COMMON
// RUN_PIPELINE_TILL: FRONTEND
// SDK 1.1.3：20261113-cjmp-body-matrix-test-writer/cjc-results.json，common-property。
// 原 probe 无 Parse 错误，Sema 唯一消息为 property 'value' can not be abstract。
package body_matrix
public common abstract class PropertyOwner {
    public common init()
    public static abstract <!MISSING_FUNC_BODY!>prop value: Int64<!>
}
