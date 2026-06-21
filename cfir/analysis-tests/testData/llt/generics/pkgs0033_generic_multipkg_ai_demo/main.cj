// FILE: pkg/pkg.cj


package pkg

internal import pkg0.*

func foo(ls: Array<String>) {
    0
}

public func M() {
    0
}

// FILE: pkg0/pkg0.cj


package pkg0

func goo(ls: Array<String>) {
    0
}

func f() {
    0
}

// FILE: main.cj



internal import pkg.*

main(): Int64 {
    M()
    return 0
}
