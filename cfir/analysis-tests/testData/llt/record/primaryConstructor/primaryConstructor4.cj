

struct A {
    public A(var a!: Int64 = 3, var b!: Int64 = a) {
        var d: Int64 = a
        this.b = d + 1
    }
}

struct B {
    public var a: Int64 = 2
    public B(a!: Int64 = 3, var b!: Int64 = a) {
        var d: Int64 = a
        this.b = d + 1
    }
}

struct C {
    public C(var a!: Int64 = 1, var b!: Int64 = a) {}
}

main(): Int64 {
    if (A().b == 4 && B().b == 4 && C(a: 2, b: 8).b == 8 && C().b == 1) {
        return 0
    }
    return 1
}
