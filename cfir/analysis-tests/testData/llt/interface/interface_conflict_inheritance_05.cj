



interface I1 {
    mut static prop a: Int64 {
        get() {
            0
        }
        set(v) {}
    }
    static func f(): Unit {}
}

interface I2 <: I1 {
    mut static prop a: Int64
    static func f(): Unit
}

main() {}

