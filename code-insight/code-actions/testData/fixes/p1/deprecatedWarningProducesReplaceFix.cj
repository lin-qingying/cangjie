// DEPRECATED_WARNING：仓颉 @Deprecated 只有 message/since/strict 参数，replacement 恒为空串。
package quickfix.p1

@Deprecated[message = "use newApi", strict = false]
func oldApi(): Unit {}

func sample() {
    oldApi()
}
