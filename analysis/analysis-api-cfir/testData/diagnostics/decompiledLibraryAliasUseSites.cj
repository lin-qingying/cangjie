// MODULE: lib
// MODULE_KIND: LibraryBinaryDecompiled
// FILE: lib.cj
package sample.lib

public type Color = SampleColor
public type Numbers = Array<Int64>
public type Handler = (Int64) -> Int64

public enum SampleColor {
    Red | Green
}

public func colorOf(): Color {
    return Color.Red
}

public func numbersOf(): Numbers {
    return [1, 2, 3]
}

public func handlerOf(): Handler {
    return { value => value + 1 }
}

// MODULE: main(lib)
// MAIN_MODULE
// MAIN_FILE_NAME: main.cj
// FILE: main.cj
package sample.main

import sample.lib.*

// 1) alias→enum 的穷尽 match 必须零诊断；非穷尽形态见 decompiledLibraryAliasNonexhaustiveMatch（正例+负例成对，才证明 pattern 解析到了真实构造器）
func colorCode(): Int64 {
    match (colorOf()) {
        case SampleColor.Red => return 1
        case SampleColor.Green => return 2
    }
}

// 2) 外层 alias→Array：extend 成员查找（size/isEmpty）
func numbersLength(): Int64 {
    let numbers = numbersOf()
    let size = numbers.size
    if (numbers.isEmpty()) {
        return 0
    }
    return size
}

// 3) alias→函数类型：函数值调用的形参映射必须先展开别名
func callHandler(): Int64 {
    let handler = handlerOf()
    return handler(41)
}
