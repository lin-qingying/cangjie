// FILE: pkg/pkg.cj


package pkg

public open class Foo {
    init() {}
}

// FILE: main.cj



internal import pkg.*

class Bar <: Foo {
    <!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>i<!>nit() {
    }
    <!NO_MATCH_FUNCTION_DECLARATION_FOR_CALL!>i<!>nit(a: Int64) {
        let aNew = 1
    }
}
