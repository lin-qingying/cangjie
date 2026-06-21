

interface I1 {}

interface I2 {}

class C1 <: I1 & I2 {}

class C2 <: I1 & I2 {}

class C3 <: I1 & I2 {}

func foo(a: Int64): Int64 {
    match (a) {
        case 1 => C1()
        case 2 => C2()
        case 3 => C3()
        case _ => ()
    }
    return 0
}

main() {
    return foo(1)
}
