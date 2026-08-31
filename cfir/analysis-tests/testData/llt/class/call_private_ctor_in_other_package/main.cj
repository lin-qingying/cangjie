// FILE: pkg/pkg.cj


package pkg

public open class Foo {
    init() {}
}

// FILE: main.cj



internal import pkg.*

class Bar <: Foo {
    <!NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS!>init<!>() {
    }
    <!NO_NON_PARAM_CONSTRUCTOR_IN_SUPER_CLASS!>init<!>(a: Int64) {
        let aNew = 1
    }
}
