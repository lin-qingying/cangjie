

package pb

struct S1 <: I1 {}

struct S2 <: I2 {}

let i = if (true) {
    S1()
} else {
    S2()
}
