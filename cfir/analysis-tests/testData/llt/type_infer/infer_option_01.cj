


// Test for option type inference
interface I {}

class A <: I {}

main() {
    let v: ?I = Some(A())
}
