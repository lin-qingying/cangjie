


struct A {
    let b: Int64 = 0
}

main() {
    let a = Some(A())
    let x = a?.b
    match (x) {
        case Some(t) => t
        case None => 1
    }
}
