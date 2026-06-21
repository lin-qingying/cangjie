

// Test for option type inference
interface I {}

class A <: I {
    func test(): Unit {
        let a: ?A = Some(A())
        let v0: ??I = <!UNABLE_TO_INFER_GENERIC_FUNC!>Some(a)<!> // error
        let v1: ???I = <!TYPE_MISMATCH!>Some(a)<!> // error
    }
}

main() {}
