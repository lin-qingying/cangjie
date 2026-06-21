// FILE: p1.cj


package p1

public interface I1 {}

public interface I2 <: I1 {}

public interface I3 <: I1 {}

public interface I4 <: I2 & I3 {}

public class A <: I4 {}

public class B <: I2 & I3 {}

// FILE: main.cj



internal import p1.*

interface IA <: I4 {}

extend A <: IA {}

interface IB <: I2 & I3 {}

extend B <: IB {}

main(): Int64 {
    0
}
