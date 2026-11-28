// 全局非成员 var 的每个真实绑定在声明与使用处具有同一 TOP_LEVEL 身份。
package test.patterns.symbols

var singleValue = 1
var (number, flag, text) = (2, true, "ready")

func useGlobals(): (Int64, (Int64, Bool, String)) {
    return (<caret_singleValue>singleValue, (<caret_number>number, <caret_flag>flag, <caret_text>text))
}
