

class A {
    public static var a: String = "C"
    public var b: String = "C"
}

extend A {
    public prop f: String {
        get() {
            return this.b
        }
    }

    public static prop f1: String {
        get() {
            return a
        }
    }
}

main(): Int64 {
    if (A().f == "C" && A.f1 == "C") {
        print("${A().f}\n")
        print("${A.f1}\n")
        return 0
    }
    return 1
}
