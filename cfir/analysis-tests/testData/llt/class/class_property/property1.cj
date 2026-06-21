

class Base12<T> {
    public var d: T
    public mut prop a: T {
        get() {
            return d
        }
        set(v) {
            d = v
            return ()
        }
    }
    public init(d: T, a: T) {
        this.d = d
        this.a = a
    }
}

main() {
    var x = Base12<Bool>(true, false)
    if (x.a == false) {
        print("${x.a}")
        return 0
    }
    return 1
}
