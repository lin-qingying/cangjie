


var f: () -> Int64 = {=> 1}

open class A {
    var x: Int64 = 123
    init() {
        f = {=> <!ILLEGAL_CAPTURE_THIS!>this<!>.x}
    }
}

abstract class B {
    var x: Int64 = 123
    init() {
        f = {=> <!ILLEGAL_CAPTURE_THIS!>this<!>.x}
    }
}

class C {
    var x: Int64 = 123
    init() {
        f = {=> this.x}
    }
}

main() {}
