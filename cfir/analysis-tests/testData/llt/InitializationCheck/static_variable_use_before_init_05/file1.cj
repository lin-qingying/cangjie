// FILE: file2.cj


open class Bar {
    static let x: Int64
    static init() {
        x = <!USED_BEFORE_INITIALIZATION!>Foo.y<!>
    }
}

// FILE: file1.cj


// Test for sub class using parent class's static member in different file
// Circular initialization

class Foo <: Bar {
    static let y: Int64
    static init() {
        y = x
    }
}

main() {
    if (Foo.y == Bar.x && Bar.x == 2) {
        return 0
    }
    return 1
}
