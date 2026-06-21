// FILE: p1.cj


package p1

public interface I1 {}

public interface I2 {}

public open class A <: I1 & I2 {}

public class B <: A {}

// FILE: main.cj



internal import p1.*

public interface I3 <: I1 & I2 {}

extend B <: I3 {}

main() {
    0
}
