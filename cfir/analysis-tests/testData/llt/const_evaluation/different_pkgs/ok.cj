// FILE: pkg.cj


package pkg

public const (a, b) = (1, 2)

public const x = 0

public const func f() {
    x
}

// FILE: ok.cj



internal import pkg.*

const func g() {
    a + b - 3 + x + f()
}

main() {
    g()
}
