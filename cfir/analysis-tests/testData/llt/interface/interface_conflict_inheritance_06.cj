



interface I1 {
    func f(): Bool {
        true
    }
}

interface I2 {
    func f(): Rune
}

class E <: Exception {}

class A <: I1 & I2 {}

main() {
}

