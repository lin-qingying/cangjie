// FILE: file2.cj


open class Bar {
    static let x: Int64
    static init() {
        x = 2
    }
}

// FILE: file1.cj



class Foo <: Bar {
    static let y: Int64
    static init() { // called first
        y = x
    }
}

main() {
    if (Foo.y == Bar.x && Bar.x == 2) {
        return 0
    }
    return 1
}
