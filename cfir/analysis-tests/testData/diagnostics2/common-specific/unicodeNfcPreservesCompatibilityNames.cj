// LANGUAGE_VERSION: 1.1.3
// RUN_PIPELINE_TILL: FRONTEND
// cjc 1.1.3：NFC 不合并兼容字符；连字和全角字母保持独立名称。
package unicode_nfc_compatibility
class ﬀ {}
class ff {}
func ｉｆ(value: ﬀ): ﬀ { value }
func `if`(value: ff): ff { value }
func check(first: ﬀ, second: ff): Unit {
    let ligature: ﬀ = ｉｆ(first)
    let ordinary: ff = `if`(second)
}
