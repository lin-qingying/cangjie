


interface I1 {}

interface I2 {}

class C1 <: I1 & I2 {}

class C2 <: I1 & I2 {}

func foo(): Int64 {
    try {
        C1()
    } catch (_) {
        C2()
    } finally {
        "A Bad Type"
    }
    return 0
}

main() {
    return foo()
}
