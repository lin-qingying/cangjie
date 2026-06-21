

interface I1 {
    func foo1(): Int32 {
        return 1
    }
}

interface I2<T> {
    func foo2(): Int32 {
        return 2
    }
}

interface I3 <: I1 & I2<Int64> {
    func foo1(): Int32 {
        return 3
    }
    func foo2(): Int32 {
        return 3
    }
}

main(): Int64 {
    return 0
}

