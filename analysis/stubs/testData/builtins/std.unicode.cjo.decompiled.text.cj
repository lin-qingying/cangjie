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
    func isLetter: Bool

    func isNumber: Bool

    func isLowerCase: Bool

    func isUpperCase: Bool

    func isTitleCase: Bool

    func isWhiteSpace: Bool

    func toUpperCase: Rune

    func toLowerCase: Rune

    func toTitleCase: Rune

    func toUpperCase(opt: std.unicode.CasingOption): Rune

    func toLowerCase(opt: std.unicode.CasingOption): Rune

    func toTitleCase(opt: std.unicode.CasingOption): Rune
}

extend Extend <: UnicodeRuneExtension {
    func isLetter: Bool

    func isNumber: Bool

    func isLowerCase: Bool

    func isUpperCase: Bool

    func isTitleCase: Bool

    func isWhiteSpace: Bool

    func toUpperCase: Rune

    func toLowerCase: Rune

    func toTitleCase: Rune

    func toUpperCase(opt: std.unicode.CasingOption): Rune

    func toLowerCase(opt: std.unicode.CasingOption): Rune

    func toTitleCase(opt: std.unicode.CasingOption): Rune
}

interface UnicodeStringExtension {
    func isBlank: Bool

    func toLower: std.core.String

    func toLower(opt: std.unicode.CasingOption): std.core.String

    func toTitle: std.core.String

    func toTitle(opt: std.unicode.CasingOption): std.core.String

    func toUpper: std.core.String

    func toUpper(opt: std.unicode.CasingOption): std.core.String

    func trim: std.core.String

    func trimRight: std.core.String

    func trimLeft: std.core.String

    func trimStart: std.core.String

    func trimEnd: std.core.String
}

extend String <: UnicodeStringExtension {
    func isBlank: Bool

    func toLower: std.core.String

    func toUpper: std.core.String

    func toTitle: std.core.String

    func toLower(opt: std.unicode.CasingOption): std.core.String

    func toUpper(opt: std.unicode.CasingOption): std.core.String

    func toTitle(opt: std.unicode.CasingOption): std.core.String

    func trim: std.core.String

    func trimLeft: std.core.String

    func trimRight: std.core.String

    func trimStart: std.core.String

    func trimEnd: std.core.String
}
