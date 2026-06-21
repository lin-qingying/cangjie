

struct A<T> {
    public var a = {=> 5}
    public func f(x: T) {
        func g() {
            this.f1(x)
        }
        var d = {=> this.f1(x)}
        return this.f1(x)
    }
    public func g() {
        this
    }
    public func f1(t: T): T {
        return t
    }
}

main(): Int64 {
    var a = A<Int64>().f(2)
    if (a == 2 && A<Int64>().g().g().a() == 5) {
        print("${a}")
        return 0
    }
    return 1
}
