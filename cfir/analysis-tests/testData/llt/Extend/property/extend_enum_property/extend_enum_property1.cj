


main(): Int64 {
    if (<!ILLEGAL_ACCESS_NON_STATIC_MEMBER!>D<!>ata.n != 1) {
        return 1
    }
    <!ILLEGAL_ACCESS_NON_STATIC_MEMBER!>D<!>ata.m = 10
    if (<!ILLEGAL_ACCESS_NON_STATIC_MEMBER!>D<!>ata.m != 2) {
        return 2
    }
    return 0
}

enum Data {
    DAY
}

extend Data {
    public <!EXTEND_IMMUTABLE_MUT_PROPERTY!>mut<!> prop m: Int64 {
        get() {
            return 2
        }
        set(v) {}
    }
    public prop n: Int64 {
        get() {
            return 1
        }
    }
}
