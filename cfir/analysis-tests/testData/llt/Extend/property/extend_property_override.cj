

interface I {
    prop pa: Int64 {
        get() {
            1
        }
    }
}

extend Int32 <: I {
    public prop pa: Int64 {
        get() {
            0
        }
    }
}

main() {
    return 1.pa
}
