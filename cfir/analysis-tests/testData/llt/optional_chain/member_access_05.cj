


struct A {
    let b: B = B()
}

struct B {
    let c: Int64 = 0
}

main() {
    let a = Some(A())
    let x = <!OPTIONAL_CHAIN_NON_OPTIONAL!>a?.b<!>?.c
}
