// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.unicode

import std.collection.*
import std.core.*

enum CasingOption {
    | TR

    | AZ

    | LT

    | Other
}

interface UnicodeRuneExtension {
    abstract open func isLetter(): Bool

    abstract open func isNumber(): Bool

    abstract open func isLowerCase(): Bool

    abstract open func isUpperCase(): Bool

    abstract open func isTitleCase(): Bool

    abstract open func isWhiteSpace(): Bool

    abstract open func toUpperCase(): Rune

    abstract open func toLowerCase(): Rune

    abstract open func toTitleCase(): Rune

    abstract open func toUpperCase(opt: std.unicode.CasingOption): Rune

    abstract open func toLowerCase(opt: std.unicode.CasingOption): Rune

    abstract open func toTitleCase(opt: std.unicode.CasingOption): Rune
}

extend Rune <: UnicodeRuneExtension {
    func isLetter(): Bool { /* compiled code */ }

    func isNumber(): Bool { /* compiled code */ }

    func isLowerCase(): Bool { /* compiled code */ }

    func isUpperCase(): Bool { /* compiled code */ }

    func isTitleCase(): Bool { /* compiled code */ }

    func isWhiteSpace(): Bool { /* compiled code */ }

    func toUpperCase(): Rune { /* compiled code */ }

    func toLowerCase(): Rune { /* compiled code */ }

    func toTitleCase(): Rune { /* compiled code */ }

    func toUpperCase(opt: std.unicode.CasingOption): Rune { /* compiled code */ }

    func toLowerCase(opt: std.unicode.CasingOption): Rune { /* compiled code */ }

    func toTitleCase(opt: std.unicode.CasingOption): Rune { /* compiled code */ }
}

interface UnicodeStringExtension {
    abstract open func isBlank(): Bool

    abstract open func toLower(): std.core.String

    abstract open func toLower(opt: std.unicode.CasingOption): std.core.String

    abstract open func toTitle(): std.core.String

    abstract open func toTitle(opt: std.unicode.CasingOption): std.core.String

    abstract open func toUpper(): std.core.String

    abstract open func toUpper(opt: std.unicode.CasingOption): std.core.String

    abstract open func trim(): std.core.String

    @Deprecated[message: "Use member function `func trimEnd(): String` instead."]
    abstract open func trimRight(): std.core.String

    @Deprecated[message: "Use member function `func trimStart(): String` instead."]
    abstract open func trimLeft(): std.core.String

    abstract open func trimStart(): std.core.String

    abstract open func trimEnd(): std.core.String
}

extend std.core.String <: UnicodeStringExtension {
    func isBlank(): Bool { /* compiled code */ }

    func toLower(): std.core.String { /* compiled code */ }

    func toUpper(): std.core.String { /* compiled code */ }

    func toTitle(): std.core.String { /* compiled code */ }

    func toLower(opt: std.unicode.CasingOption): std.core.String { /* compiled code */ }

    func toUpper(opt: std.unicode.CasingOption): std.core.String { /* compiled code */ }

    func toTitle(opt: std.unicode.CasingOption): std.core.String { /* compiled code */ }

    func trim(): std.core.String { /* compiled code */ }

    @Deprecated[message: "Use member function ` public func trimStart(): String` instead."]
    func trimLeft(): std.core.String { /* compiled code */ }

    @Deprecated[message: "Use member function ` public func trimEnd(): String` instead."]
    func trimRight(): std.core.String { /* compiled code */ }

    func trimStart(): std.core.String { /* compiled code */ }

    func trimEnd(): std.core.String { /* compiled code */ }
}
