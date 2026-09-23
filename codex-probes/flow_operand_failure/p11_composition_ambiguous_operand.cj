struct Sample {
    public func mono(a: Int64): Int64 { a }
    public func overloaded(a: Int64): Int64 { a }
    public func overloaded(a: Bool): Int64 { 1 }
}

main() {
    let s = Sample()
    let r = s.overloaded ~> s.mono
    r
}
