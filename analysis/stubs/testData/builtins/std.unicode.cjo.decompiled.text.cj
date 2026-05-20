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
    abstract func isLetter(): Bool

    abstract func isNumber(): Bool

    abstract func isLowerCase(): Bool

    abstract func isUpperCase(): Bool

    abstract func isTitleCase(): Bool

    abstract func isWhiteSpace(): Bool

    abstract func toUpperCase(): Rune

    abstract func toLowerCase(): Rune

    abstract func toTitleCase(): Rune

    abstract func toUpperCase(opt: std.unicode.CasingOption): Rune

    abstract func toLowerCase(opt: std.unicode.CasingOption): Rune

    abstract func toTitleCase(opt: std.unicode.CasingOption): Rune
}

extend `Unit` <: UnicodeRuneExtension {
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
    abstract func isBlank(): Bool

    abstract func toLower(): std.core.String

    abstract func toLower(opt: std.unicode.CasingOption): std.core.String

    abstract func toTitle(): std.core.String

    abstract func toTitle(opt: std.unicode.CasingOption): std.core.String

    abstract func toUpper(): std.core.String

    abstract func toUpper(opt: std.unicode.CasingOption): std.core.String

    abstract func trim(): std.core.String

    abstract func trimRight(): std.core.String

    abstract func trimLeft(): std.core.String

    abstract func trimStart(): std.core.String

    abstract func trimEnd(): std.core.String
}

extend String <: UnicodeStringExtension {
    func isBlank(): Bool { /* compiled code */ }

    func toLower(): std.core.String { /* compiled code */ }

    func toUpper(): std.core.String { /* compiled code */ }

    func toTitle(): std.core.String { /* compiled code */ }

    func toLower(opt: std.unicode.CasingOption): std.core.String { /* compiled code */ }

    func toUpper(opt: std.unicode.CasingOption): std.core.String { /* compiled code */ }

    func toTitle(opt: std.unicode.CasingOption): std.core.String { /* compiled code */ }

    func trim(): std.core.String { /* compiled code */ }

    func trimLeft(): std.core.String { /* compiled code */ }

    func trimRight(): std.core.String { /* compiled code */ }

    func trimStart(): std.core.String { /* compiled code */ }

    func trimEnd(): std.core.String { /* compiled code */ }
}
