

interface Boo {
    mut prop m: Int32 {
        get() {
            return 1
        }
        set(input) {
            ()
        }
    }
}

class Coo <: Boo {
    public mut override prop m: Int32 {
        get() {
            return (10)
        }
        set(input) {
            ()
        }
    }
}

main() {
    return Int64(Coo().m - 10)
}
