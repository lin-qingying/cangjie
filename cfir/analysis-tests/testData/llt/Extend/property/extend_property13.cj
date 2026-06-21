

main(): Int64 {
    var a: Int64 = 1
    if (a.n != 1) {
        return 1
    }
    if (a.m != 2) {
        return 2
    }

    if (1.n != 1) {
        return 1
    }
    if (1.m != 2) {
        return 2
    }
    return 0
}

extend Int64 {
    public prop m: Int64 {
        get() {
            return 2
        }
    }
    public prop n: Int64 {
        get() {
            return 1
        }
    }
}
