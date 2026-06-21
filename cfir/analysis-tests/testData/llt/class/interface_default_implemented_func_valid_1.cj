

interface A {
    mut func c(b: Int64): Int64

    mut func cc(b: Int64): Int64 {
        c(b)
    }
}

struct R <: A {
    public var a = 3
    public mut func c(b: Int64): Int64 {
        this.a = b
        return this.a
    }
}

main() {
    var a = R()
    a.cc(9)
    return (a.a - 9)
}
