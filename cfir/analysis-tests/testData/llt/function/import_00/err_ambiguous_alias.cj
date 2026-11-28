// DIAGNOSTICS: -warnings
// FILE: m1/p1.cj

package m1.p1

public func f() {
    1
}

// FILE: m2/p1.cj

package m2.p1

public func f() {
    11
}

// FILE: err_ambiguous_alias.cj

import m1.p1 as shared
import m2.p1 as shared

main() {
    <!AMBIGUOUS_USE!>shared<!>.f()
}
