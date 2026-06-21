


open class A {}

interface I {}

open class B <: A & I {}

interface I2 <: I {}

interface I5 <: I {}

interface Base <: I2 & I5 {}

func foo1(a: I2): Unit {}

func foo2(a: I5): Unit {}

main() {
    let a = if (true) {
        foo1
    } else {
        foo2
    }
    if (a is Any) {
        return 0
    }
    return 1
}
