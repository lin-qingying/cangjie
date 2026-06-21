

struct A<T> {
    public var a = {=> 5}
    public func f() {
        func g() {
            this.f1()
        }
        var x = {=> this.f1()}
        if (this.f1() == 2) {
            return this.f1() + this.a() + g() + x()
        }
        return 0
    }
    public func g() {
        this
    }
    public func f1() {
        return 2
    }
}

main(): Int64 {
    var a = A<Int64>().f()
    if (a == 11 && A<Int64>().g().g().a() == 5) {
        print("${a}")
        return 0
    }
    return 1
}
