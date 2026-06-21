

// Was failed, should pass
interface I1 {
    func f(): Bool
}

interface I2 {
    func f(): Rune
}

interface I3 <: I1 & I2 {
    func f(): Nothing
}

interface I4 <: I3 {
    func f(): Nothing
}

class E <: Exception {}

class A <: I1 & I2 {
    public func f(): Nothing {
        throw E()
    }
}

main() {}
