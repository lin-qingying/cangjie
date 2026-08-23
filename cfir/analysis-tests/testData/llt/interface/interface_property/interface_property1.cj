



interface A {
    mut static prop v: Int64 {
        get() {
            return 2
        }
        set(v) {}
    }
}

interface B <: A {
    mut static prop v: Int64 {
        set(v) {
        }
    }
}

main(): Int64 {
    return 0
}

