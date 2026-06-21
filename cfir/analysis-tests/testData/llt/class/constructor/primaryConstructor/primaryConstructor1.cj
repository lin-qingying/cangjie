

open class B<X, Y> {
    public B(x: Int64, y: X, public var z: Y) {}
    public init(z: Y) {
        this.z = z
    }
}

class C <: B<Int64, Int64> {
    public C() {
        super(1, 2, 3)
    }
}

open class D {
    protected D(var z!: Int64 = 3) {
        this.z = z + 2
    }
}

class E <: D {
    public E() {
        var c: Int64 = 2
    }
}

class F {
    public static var c = 3
    public F(var d!: Int64 = c) {}
}

class G {
    public G(var a: Int64, var b!: Int64 = a) {
        super()
    }
}

main(): Int64 {
    B<Int64, Int64>(4).z
    G(2).b
    if (B<Int64, Int64>(4).z == 4 && B<Int64, String>(1, 2, "str").z == "str" && C().z == 3 && E().z == 5 && F().d == 3) {
        return 0
    }
    print("${B<Int64, Int64>(4).z} == 4\n");
    print("${B<Int64, String>(1, 2, "str").z} == str\n")
    print("${C().z} == 3\n")
    print("${E().z} == 5 \n")
    print("${F().d} == 3 \n")
    return 1
}
