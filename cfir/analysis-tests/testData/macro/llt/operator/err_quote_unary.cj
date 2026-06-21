

import std.ast.*

main() {
    quote($(<!INVALID_UNARY_EXPR!>-"")<!>)
}
