struct Sample {
    public func mono(a: Int64): Int64 { a }
    public func overloaded(a: Int64): Int64 { a }
}

main() {
    let s = Sample()
    let r = unknownReceiver.overloaded ~> s.overloaded
    r
}
