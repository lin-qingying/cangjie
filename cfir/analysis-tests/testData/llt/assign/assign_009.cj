

interface I {
    mut prop b: Int64 {
        get() {
            0
        }
        set(v) {
            <!CANNOT_ASSIGN_TO_IMMUTABLE!>v<!> = v
        }
    }
}

main() {}
