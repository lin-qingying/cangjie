

interface I1 {}

interface I2 <: I1 {}

interface I3 {}

interface I4 <: I3 {}

class A {}

extend A <: I1 & I3 {}

extend A <: I2 & I4 {}

main() {
    0
}
