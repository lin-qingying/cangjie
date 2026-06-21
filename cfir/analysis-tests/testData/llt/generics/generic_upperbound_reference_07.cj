


interface I1 {
    prop a: Int64
}

interface I2 {
    prop a: Int64
}

class A <: I1 & I2 {
    public prop a: Int64 {
        get() {
            3
        }
    }
}

func test<T>(i: T) where T <: I1 & I2 {
    let x = i.a
    return x
}

main() {
    let ins = A()
    let v = test(ins)
    if (v != ins.a) {
        return 1
    }
    return 0
}
