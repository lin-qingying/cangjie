


func test<T>(a: (Int64, T)) {
    match (a) {
        case (_, v: Int64) => v
        case _ => 0
    }
}

main() {
    let val = 22
    let v = test((1, val))
    if (v != val) {
        return 1
    }
    return 0
}
