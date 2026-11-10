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

