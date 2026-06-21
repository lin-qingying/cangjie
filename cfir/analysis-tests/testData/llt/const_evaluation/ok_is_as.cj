


enum E {
    A
}

const func f() {
    let x = A
    let y = x as E
    let z = y is Option<E>
    const func g(x: Option<E>) {
        match (x) {
            case Some(A) => 0
            case None => 1
        }
    }
    if (z) {
        return g(x)
    }
    return 2
}

main() {
    f()
}
