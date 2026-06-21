// FILE: test1.cj


package p1

interface I<T> {}

class C1 {}

class C2<T> <: I<T> {
    let x: C1 = C1()
}

public func f<T>() {
    let y: I<T> = C2<T>()
}

// FILE: test2.cj



package p2

internal import p1.*

public let X = f<Any>()

main() {}
