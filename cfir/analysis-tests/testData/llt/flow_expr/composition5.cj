


class A {
    public operator func ()(x: Int32): Int64 {
        Int64(x)
    }
}

class B {
    public operator func ()(x: Int64): Int32 {
        Int32(x)
    }
}

func run() {
    let x: Int32 = 1
    let a: A = A()
    let b: B = B()
    let f = a ~> b
    let res = f(x)
    print("res: ${res}")
    return res
}

main(): Int64 {
    let res = run()
    if (res == 1) {
        return 0
    }
    return 1
}
