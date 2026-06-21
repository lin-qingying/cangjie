

main(): Int64 {
    if (foo<Int64>(1) != 3) {
        return 1
    }
    return 0
}

func foo<<!UPPER_BOUND_MUST_BE_CLASS_OR_INTERFACE!>T<!>>(x: T): Int32 where T <: Int64 {
    return x.n + x.m
}

extend Int64 {
    public prop m: Int32 {
        get() {
            return 2
        }
    }
    public prop n: Int32 {
        get() {
            return 1
        }
    }
}
