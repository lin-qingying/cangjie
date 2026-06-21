// FILE: p1.cj


package p1

public interface I1 {}

public interface I2 <: I1 {}

public class A <: I2 {}

// FILE: main.cj



internal import p1.*

interface I <: I2 {}

extend A <: I {}

main(): Int64 {
    0
}
