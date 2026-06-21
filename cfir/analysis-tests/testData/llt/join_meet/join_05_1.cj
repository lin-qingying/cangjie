


interface I1 {}

interface I2 {}

interface I3 {}

interface I4 {}

interface I5 {}

interface I6 {}

struct Cx <: I2 & I3 {}

struct A <: I1 & I2 & I3 {}

struct B <: I3 & I4 & I5 & I6 {}

main() {
    let x1 = [Cx(), A(), B()]
    let x2 = [Cx(), B(), A()]
    let x3 = [A(), Cx(), B()]
    let x4 = [A(), B(), Cx()]
    let x5 = [B(), Cx(), A()]
    let x6 = [B(), A(), Cx()]

    println(x1 is Array<I3>)
    println(x2 is Array<I3>)
    println(x3 is Array<I3>)
    println(x4 is Array<I3>)
    println(x5 is Array<I3>)
    println(x6 is Array<I3>)
    0
}
