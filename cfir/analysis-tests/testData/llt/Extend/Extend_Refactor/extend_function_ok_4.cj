

interface I1 {
    func foo(): Int64 {
        0
    }
}

interface I2 <: I1 {
    func foo(): Int64 {
        1
    }
}

class C1 {}

extend C1 <: I2 { // ok

}

main() {
    return 0
}
