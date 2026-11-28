// MODULE: lib
// MODULE_KIND: LibraryBinaryDecompiled
// FILE: lib.cj
package sample.lib

public type Color = SampleColor

public enum SampleColor {
    Red | Green | Blue
}

public func colorOf(): Color {
    return Color.Red
}

// MODULE: main(lib)
// MAIN_MODULE
// MAIN_FILE_NAME: main.cj
// FILE: main.cj
package sample.main

import sample.lib.*

// 非穷尽 match：别名必须展开成 enum 后才能看到 Green/Blue 缺失。
// 若检查器因别名未展开而 Skipped，这里会错误地零诊断。
func missingCases(value: Int64): Int64 {
    match (colorOf()) {
        case SampleColor.Red => return value
    }
}
