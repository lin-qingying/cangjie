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

extend Extend <: UnicodeRuneExtension {
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
    func isBlank(): Bool { /* compiled code */ }

    func toLower(): std.core.String { /* compiled code */ }

    func toLower(opt: std.unicode.CasingOption): std.core.String { /* compiled code */ }

    func toTitle(): std.core.String { /* compiled code */ }

    func toTitle(opt: std.unicode.CasingOption): std.core.String { /* compiled code */ }

    func toUpper(): std.core.String { /* compiled code */ }

    func toUpper(opt: std.unicode.CasingOption): std.core.String { /* compiled code */ }

    func trim(): std.core.String { /* compiled code */ }

    func trimRight(): std.core.String { /* compiled code */ }

    func trimLeft(): std.core.String { /* compiled code */ }

    func trimStart(): std.core.String { /* compiled code */ }

    func trimEnd(): std.core.String { /* compiled code */ }
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
