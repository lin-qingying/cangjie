// LANGUAGE_VERSION: 1.1.3
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3 在类型模式中把反引号声明与 NFC 等价引用视为同一绑定。
package unicode_pattern
func check(value: Int64): Int64 {
    match (value) {
        case `café`: Int64 => café
    }
}
