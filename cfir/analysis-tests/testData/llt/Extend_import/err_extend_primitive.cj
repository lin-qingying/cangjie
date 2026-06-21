

// DIAGNOSTICS: -warnings

import std.convert.Formattable
import std.math.abs

main() {
    println(<!UNRESOLVED_REFERENCE!>pow<!>(1.2, 2.3))
}
