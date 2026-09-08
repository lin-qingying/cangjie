// FILE: base.cj
package base
public class Foo {}

// FILE: a.cj
package a
import base.Foo
public interface IA {
    prop code: Int64 { get() { 1 } }
}
<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>extend<!> Foo <: IA {}

// FILE: b.cj
package b
import base.Foo
public interface IB {
    prop code: Int64 { get() { 2 } }
}
<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>extend<!> Foo <: IB {}

// FILE: main.cj
import base.Foo
import <!UNUSED_IMPORT!>a.IA<!>
import <!UNUSED_IMPORT!>b.IB<!>
main() {
    Foo()
    return 0
}
