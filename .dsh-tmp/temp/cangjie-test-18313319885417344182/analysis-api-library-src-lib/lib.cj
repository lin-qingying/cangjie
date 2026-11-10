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

