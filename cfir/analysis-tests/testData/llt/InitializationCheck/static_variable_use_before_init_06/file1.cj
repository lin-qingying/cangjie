// FILE: file2.cj


open class Bar {
    static let x: Int64
    static init() {
        x = Foo.y
    }
}

// FILE: file1.cj



class Foo <: Bar {
    static var y: Int64
    static init() {
        y = 1
    }
}

main() {
    if (Foo.y == Bar.x && Bar.x == 1) {
        return 0
    }
    return 1
}
