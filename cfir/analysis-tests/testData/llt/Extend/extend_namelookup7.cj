

class A {
    public A(var a!: Int64 = 5) {}
}

extend A {
    public func me() {
        return ppp()
    }
    public func ppp() {
        return this.a
    }
}

main(): Int64 {
    return A().me() - 5
}
