// FILE: A.cj


package A

public class Foo {}

// FILE: B.cj


package B

internal import A.Foo

public interface I1 {
    func f(): Unit {}
}

extend Foo <: I1 {}

// FILE: C.cj



package C

internal import A.Foo
internal import B.I1

public interface I2 {
    func f(): Unit {}
}

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>extend<!> Foo <: I2 {}
