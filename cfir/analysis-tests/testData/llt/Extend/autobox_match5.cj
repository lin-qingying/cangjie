

interface I {}

struct R <: I {
    func f<T>() {}
}

struct R2 {
    func f<T>() {}
}

main() {
    let a = R() as I
    match (a) {
        case Some(x) => ()
        case None => return 1
    }
    match (R2() as I) {
        case Some(x) => return 2
        case None => ()
    }
    return 0
}
