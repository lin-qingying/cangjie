

main(): Int64 {
    if (Data.DAY.n != 1) {
        return 1
    }
    if (Data.DAY.m != 2) {
        return 2
    }
    return 0
}

enum Data {
    DAY
}

extend Data {
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
