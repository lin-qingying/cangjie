


struct A {
    let b: B = B()
}

struct B {
    let c: Int64 = 0
}

main() {
    let a = Some(A())
    let x = a?.b.c
    match (x) {
        case Some(t) => t
        case None => 1
    }
}
