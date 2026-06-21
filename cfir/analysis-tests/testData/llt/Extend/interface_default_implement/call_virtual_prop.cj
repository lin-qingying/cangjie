

interface I {
    prop b: Int64
    prop b1: Int64 {
        get() {
            b
        }
    }
}

struct S <: I {
    public prop b: Int64 {
        get() {
            1
        }
    }
}

main() {
    return S().b1 - 1
}
