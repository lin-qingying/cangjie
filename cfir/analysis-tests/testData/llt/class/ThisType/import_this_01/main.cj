// FILE: pkg.cj


package pkg

public open class A {
    public func foo(): This {
        this
    }
}

public open class B <: A {
    public func goo(): This {
        this
    }
}

// FILE: main.cj



internal import pkg.*

main() {
    B().foo().goo()
    return
}
