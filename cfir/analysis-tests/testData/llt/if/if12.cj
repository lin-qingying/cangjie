


interface I1 {}

interface I2 {}

class C1 <: I1 & I2 {}

class C2 <: I1 & I2 {}

class C3 <: I1 & I2 {}

func foo(): Int64 {
    if (true) {
        C1()
    } else if (true) {
        C2()
    } else {
        C3()
    }
    return 0
}

main() {
    return foo()
}
