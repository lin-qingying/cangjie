interface I {}
class A <: I {}

func checkPayload() {
    let a: ?A = Some(A())
    let p0: ?I = Some(A())
    let p1: ??I = <!UNABLE_TO_INFER_GENERIC_FUNC!>Some<!>(a)
    let p2: ???I = <!TYPE_MISMATCH!>Some(a)<!>
    let p3: ??I = <!TYPE_MISMATCH!>Some(A())<!>
    let p4: ???I = <!TYPE_MISMATCH!>Some(A())<!>
    let p5: ???A = Some(a)
    let p6: ??A = Some(None)
    let p7: ???A = Some(None)
}

main() { 0 }
