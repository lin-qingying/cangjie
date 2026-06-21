// FILE: a.cj


package pa

import std.collection.*
import <!UNUSED_IMPORT!>std.unittest<!>
import <!UNUSED_IMPORT!>std.unittest.testmacro<!>

func foo() {
    return ["", ArrayList<Any>()]
}

// FILE: test.cj



package pa

private import std.ast.*

main() {
    let arr: Array<ToTokens> = <!TYPE_MISMATCH!>foo()<!>
}
