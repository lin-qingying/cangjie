


interface I0 {}

interface I1 <: I0 {}

interface I2 <: I0 {}

open class C1 <: I1 & I2 {
    func h(a: I1): Int64 {
        1
    }
    func h(a: C1): Int64 {
        2
    }
}

class C2 <: C1 & I2 {
    func h(a: I2): Int64 {
        1
    }
    func h(a: C2): Int64 {
        2
    }
}

func f<T>(a: T): Int64 where T <: C1 & C2 {
    var lam1: (I1) -> Int64 = a.h
    var lam2: (I2) -> Int64 = a.h
    if (lam1(a) != 1) {
        return 1
    } else if (lam2(a) != 1) {
        return 2
    } else {
        return 0
    }
}

main(): Int64 {
    return f<C2>(C2())
}
