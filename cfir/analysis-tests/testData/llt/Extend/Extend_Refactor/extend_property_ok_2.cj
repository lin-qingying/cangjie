

interface I1 {
    prop foo: Int64 {
        get() {
            return 1
        }
    }
}

interface I2 {
    prop foo: Int64 {
        get() {
            return 2
        }
    }
}

class C1 {
    public prop foo: Int64 {
        get() {
            return 3
        }
    }
}

extend C1 <: I1 & I2 {} // ok

main() {
    return 0
}
