// FILE: pkg.cj


package pkg

public open class A<T> {
    @Frozen
    mut prop x: Int64 {
        get() {
            0
        }
        set(v) {}
    }

    @Frozen
    func foo(): Int64 {
        0
    }
}

public func bar<T>(t: A<T>) {
    t.x + t.foo()
}

// FILE: main.cj



import pkg.*

main() {
    var a = A<Int64>()
    bar(a)
}
