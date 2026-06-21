// FILE: A.cj
package A
public class Foo { }




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

public interface I2 {
    func f(): Unit {}
}

extend Foo <: I2 {}




// FILE: main.cj
internal import A.Foo
internal import B.I1
internal import C.I2

main(): Int64 {
    let a = Foo()
    a.<!AMBIGUOUS_FUNCTION_CALL!>f<!>()
    return 1
}
