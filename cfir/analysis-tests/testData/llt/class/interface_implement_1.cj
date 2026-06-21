

interface I1 {
    func foo(): Int64
}

interface I2 <: I1 {
    func foo(): Int64 {
        return 2
    }
}

interface I3 <: I1 {}

class C1 <: I2 & I3 {}

main(): Int64 {
    var res = C1().foo()
    if (res == 2) {
        return 0
    }
    return 1
}

