

interface I1 {
    func f(a: Int64): Int64 {
        return 1
    }
}

interface I2 {
    func f(a: Int64): Int64 {
        return 2
    }
}

open class A {
    public func f(a: Int64): Int64 {
        return 3
    }
}

open class B <: A & I1 & I2 { // ok, f from A

}

main() {
    var obj = B()
    var res = obj.f(1)
    if (res == 3) {
        return 0
    }
    return 1
}
