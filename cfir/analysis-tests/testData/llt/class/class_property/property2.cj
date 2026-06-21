

class Base12<T> {
    public var <!CLASS_UNINITIALIZED_FIELD!>d<!>: T
    public mut prop a: T {
        get() {
            return d
        }
        set(v) {
            d = v
        }
    }
}

main() {
    var x = Base12<Int64>()
    x.a = 4
    if (x.a == 4) {
        print("${x.a}")
        return 0
    }
    return 1
}
