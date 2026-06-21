


func foo(b: Int64) {
    b - 1
}

main() {
    let a = Some(foo)
    let b = 1
    let x = a?(b)
    match (x) {
        case Some(t) => t
        case None => 1
    }
}
