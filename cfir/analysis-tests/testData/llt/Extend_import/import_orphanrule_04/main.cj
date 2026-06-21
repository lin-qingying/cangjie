// FILE: p1.cj


package p1

public interface I1 {}

public interface I2 {}

public struct A <: I1 & I2 {}

// FILE: main.cj



internal import p1.*

public interface I3 <: I1 & I2 {}

extend A <: I3 {}

main() {
    0
}
