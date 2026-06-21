

class Foo <: Bar {
    static let y: Int64
    static init() { // called first
        z
        y = <!USED_BEFORE_INITIALIZATION!>x<!> // error: not yet initialized variable
    }
    static let z = 1
}

open class Bar {
    static let x: Int64
    static init() { // called second
        x = 2
    }
}

var b = Bar.x

main() {
    println(Foo.y)
    println(Bar.x)
}
