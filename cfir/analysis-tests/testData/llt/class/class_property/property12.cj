


main(): Int64 {
    var a = Doo()
    if (a.m != 10) {
        return 1
    }
    if (a.n != 20) {
        return 2
    }
    a.m = 1
    if (a.m != 10) {
        return 3
    }
    if (a.n != 20) {
        return 4
    }

    return 0
}

interface Boo {
    mut prop m: Int32 {
        get() {
            return 1
        }
        set(input) {
            ()
        }
    }

    prop n: Int32 {
        get() {
            return (2)
        }
    }
}

interface Coo <: Boo {
    mut prop m: Int32 {
        get() {
            return (10)
        }
        set(input) {
            ()
        }
    }
    prop n: Int32 {
        get() {
            return 20
        }
    }
}

class Doo <: Coo {}
