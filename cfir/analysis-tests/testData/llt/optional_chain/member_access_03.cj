


struct A {
    let b: B = B()
}

struct B {
    let c: Option<C> = C()
}

struct C {
    let d: Int64 = 0
}

main() {
    let a = Some(A())
    let x = a?.b.c?.d
    match (x) {
        case Some(t) => t
        case None => 1
    }
}
