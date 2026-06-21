

package a.b

private interface I1 { // external
    public func f1() {
        1
    } // external
}

internal interface I2 { // external
    public func f1() {
        1
    } // external
}

protected interface I3 { // external
    public func f1() {
        1
    } // external
}

public class GGGG {}

extend GGGG <: I1 {}

public class HHHH {}

extend HHHH <: I2 {}

public class IIII {}

extend IIII <: I3 {}
