

main(): Int64 {
    if (Int8.ns != 3) {
        return 3
    }
    if (Int8.ms != 4) {
        return 4
    }
    return 0
}

extend Int8 {
    public static prop ms: Int64 {
        get() {
            return 4
        }
    }
    public static prop ns: Int64 {
        get() {
            return 3
        }
    }
}
