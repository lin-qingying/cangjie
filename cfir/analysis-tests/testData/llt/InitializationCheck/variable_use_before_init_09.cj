

public func test(): Option<Int64> {
    None
}

main() {
    var a: Int64
    let b = test()
    match (b) {
        case Some(v) => return 1
        case _ => a = 0
    }
    return a
}
