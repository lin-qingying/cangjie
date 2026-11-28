// IntelliJ API Decompiler stub source generated from a .cjo file
// Implementation of declarations is not available

package std.unicode

import std.collection.*
import std.core.*

public enum CasingOption {
    | TR

    | AZ

    | LT

    | Other
}

public interface UnicodeRuneExtension {
    public open func isLetter(): Bool

    public open func isNumber(): Bool

    public open func isLowerCase(): Bool

    public open func isUpperCase(): Bool

    public open func isTitleCase(): Bool

    public open func isWhiteSpace(): Bool

    public open func toUpperCase(): Rune

    public open func toLowerCase(): Rune

    public open func toTitleCase(): Rune

    public open func toUpperCase(opt: std.unicode.CasingOption): Rune

    public open func toLowerCase(opt: std.unicode.CasingOption): Rune

    public open func toTitleCase(opt: std.unicode.CasingOption): Rune
}

extend Rune <: UnicodeRuneExtension {
    public func isLetter(): Bool { /* compiled code */ }

    public func isNumber(): Bool { /* compiled code */ }

    public func isLowerCase(): Bool { /* compiled code */ }

    public func isUpperCase(): Bool { /* compiled code */ }

    public func isTitleCase(): Bool { /* compiled code */ }

    public func isWhiteSpace(): Bool { /* compiled code */ }

    public func toUpperCase(): Rune { /* compiled code */ }

    public func toLowerCase(): Rune { /* compiled code */ }

    public func toTitleCase(): Rune { /* compiled code */ }

    public func toUpperCase(opt: std.unicode.CasingOption): Rune { /* compiled code */ }

    public func toLowerCase(opt: std.unicode.CasingOption): Rune { /* compiled code */ }

    public func toTitleCase(opt: std.unicode.CasingOption): Rune { /* compiled code */ }
}

public interface UnicodeStringExtension {
    public open func isBlank(): Bool

    public open func toLower(): std.core.String

    public open func toLower(opt: std.unicode.CasingOption): std.core.String

    public open func toTitle(): std.core.String

    public open func toTitle(opt: std.unicode.CasingOption): std.core.String

    public open func toUpper(): std.core.String

    public open func toUpper(opt: std.unicode.CasingOption): std.core.String

    public open func trim(): std.core.String

    @Deprecated[message: "Use member function `func trimEnd(): String` instead."]
    public open func trimRight(): std.core.String

    @Deprecated[message: "Use member function `func trimStart(): String` instead."]
    public open func trimLeft(): std.core.String

    public open func trimStart(): std.core.String

    public open func trimEnd(): std.core.String
}

extend std.core.String <: UnicodeStringExtension {
    public func isBlank(): Bool { /* compiled code */ }

    public func toLower(): std.core.String { /* compiled code */ }

    public func toUpper(): std.core.String { /* compiled code */ }

    public func toTitle(): std.core.String { /* compiled code */ }

    public func toLower(opt: std.unicode.CasingOption): std.core.String { /* compiled code */ }

    public func toUpper(opt: std.unicode.CasingOption): std.core.String { /* compiled code */ }

    public func toTitle(opt: std.unicode.CasingOption): std.core.String { /* compiled code */ }

    public func trim(): std.core.String { /* compiled code */ }

    @Deprecated[message: "Use member function ` public func trimStart(): String` instead."]
    public func trimLeft(): std.core.String { /* compiled code */ }

    @Deprecated[message: "Use member function ` public func trimEnd(): String` instead."]
    public func trimRight(): std.core.String { /* compiled code */ }

    public func trimStart(): std.core.String { /* compiled code */ }

    public func trimEnd(): std.core.String { /* compiled code */ }
}
