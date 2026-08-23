



interface I {}

class A {}

extend A <: I {}

main() {
    let a = (A(), A())
    let a1: (I, I) = a

    let b = {x: I => A()}
    let b1: (A) -> I = b
}

