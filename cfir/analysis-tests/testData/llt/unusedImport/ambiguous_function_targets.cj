// FILE: one.cj
package one
public func pick(x: Int64): Int64 { x }
public func choose(x: Int64): Int64 { x }
public interface R1 {}
public interface R2 {}
public class Both <: R1 & R2 {}
public func select(x: R1): R1 { x }
public func select(x: R2): R2 { x }

// FILE: two.cj
package two
public func pick(x: Int64): Int64 { x }
public func choose(x: String): String { x }

// FILE: unused.cj
package unused
public class Unused {}

// FILE: ambiguous.cj
package ambiguous
import one.*
import two.*
import <!UNUSED_IMPORT!>unused.Unused<!>
main() {
    <!AMBIGUOUS_FUNCTION_CALL!>pick<!>(1)
    return 0
}

// FILE: no_match.cj
package nomatch
import one.*
import two.*
import <!UNUSED_IMPORT!>unused.Unused<!>
main() {
    pick(<!TYPE_MISMATCH!>"x"<!>)
    return 0
}

// FILE: reference.cj
package reference
import <!UNUSED_IMPORT!>one.*<!>
import <!UNUSED_IMPORT!>two.*<!>
import <!UNUSED_IMPORT!>unused.Unused<!>
main() {
    let fn = <!AMBIGUOUS_USE!>pick<!>
    return 0
}

// FILE: selected.cj
package selected
import one.*
import two.*
main() {
    choose(1)
    return 0
}

// FILE: explicit.cj
package explicit
import one.Both
import one.select
main() {
    <!AMBIGUOUS_FUNCTION_CALL!>select<!>(Both())
    return 0
}

// FILE: alias.cj
package alias
import one.Both
import one.select as choose
main() {
    <!AMBIGUOUS_FUNCTION_CALL!>choose<!>(Both())
    return 0
}

// FILE: explicit_no_match.cj
package explicitnomatch
import one.select
main() {
    select(<!TYPE_MISMATCH!>"x"<!>)
    return 0
}
