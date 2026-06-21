

class A {}

interface I {}

extend TA <: TI {
    public func f() {
        2
    }
}

main() {
    return A().f() - 2
}

type TA = A

type TI = I
