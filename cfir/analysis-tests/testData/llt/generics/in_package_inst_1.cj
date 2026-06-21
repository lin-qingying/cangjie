


interface IA {
    mut static prop p: Int64 {
        get() {
            1
        }
        set(v) {
            v
        }
    }
}

struct Data<T> <: IA {
    static var _v = 0
    public mut static prop p: Int64 {
        get() {
            _v
        }
        set(v) {
            _v = v
        }
    }
    public mut func foo() {
        0
    }
}

interface I<M> {
    static func sFoo(): Int64 {
        0
    }
}

enum E<M> <: I<M> where M <: IA {
    E1(M)

    public static func sFoo() {
        M.p
    }
}

main() {
    E<Data<Int64>>.sFoo()
}
