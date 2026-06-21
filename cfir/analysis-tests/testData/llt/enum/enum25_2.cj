

enum A<T> {
    a(T, T, T)

    public prop c: Int64 {
        get() {
            1
        }
    }
    public func b() {}
    public func b(x: Int64, y: Int64, z: Int64) {}
    public func b(x: Int64) {}
    public func b(x: Float32) {}
}

main(): Int64 {
    var i = A.a(1, 2, 3)
    return i.c - 1
}
