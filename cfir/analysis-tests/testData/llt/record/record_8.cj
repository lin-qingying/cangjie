

struct A {
    private static var x: Int64 = 0
    public mut static prop a: Int64 {
        get() {
            return x
        }
        set(v) {
            x = v
        }
    }
}

main() {
    var test = A()
    A.a = 4
    var b = A.a
    if (b == 4) {
        print("${A.a}")
        return 0
    }
    return 1
}
