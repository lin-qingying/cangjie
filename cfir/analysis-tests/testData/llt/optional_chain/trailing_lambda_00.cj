


func foo(b: Int64, f: (Int64) -> Int64) {
    f(b)
}

main() {
    let a = Some(foo)
    let b = 1
    let x = a?(b) {x => x - 1}
    match (x) {
        case Some(t) => t
        case None => 1
    }
}
