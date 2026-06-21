


interface I {
    func test(): Bool {
        match (this) {
            case v: ?Int64 => (v?.test()).getOrThrow()
            case v: Int64 => true
            case _ => false
        }
    }
}

extend Int64 <: I {}

extend<T> Option<T> <: I {}

main() {
    let a: ?Int64 = 1
    if (a.test()) {
        return 0
    }
    return 1
}
