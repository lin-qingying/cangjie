// FILE: pkg.cj


package pkg

public open class A<T> {
    public func foo(): This {
        this
    }
}

// FILE: main.cj



internal import pkg.*

main() {
    A<Int64>().foo()
    return
}
