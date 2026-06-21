


struct B {
    let c: Int64 = 1
}

func foo(b: Option<Int64>) {
    match (b) {
        case Some(_) => 0
        case None => 1
    }
}

main() {
    let a = Some(foo)
    let b = Some(B())
    let x = a?(b?.c)
    match (x) {
        case Some(t) => t
        case None => 1
    }
}
