


class A {
    public operator func ()(a: (Int64) -> Int64) {
        0
    }
}

main(): Int64 {
    var a = A()
    return a {i: Int64 => i}
}
