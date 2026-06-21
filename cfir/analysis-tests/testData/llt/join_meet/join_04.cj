


interface I5 {}

interface I2 {}

interface I6 <: I2 & I5 {}

interface I4 <: I5 & I6 {}

interface I3 <: I5 & I4 {}

interface I1 <: I4 & I2 {}

struct A <: I1 & I2 {}

struct B <: I3 & I4 {}

main() {
    let x = [A(), B()]
    println(x.size)
}
