

class Coo {
    public mut prop d: Int32 {
        get() {
            d = 2
            return 12
        }
        set(input) {
            ()
        }
    }
}

main(): Int64 {
    if (Coo().d == 12) {
        return 0
    }
    return 1
}
