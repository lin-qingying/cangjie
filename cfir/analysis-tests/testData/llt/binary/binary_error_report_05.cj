

// DIAGNOSTICS: -warnings


main(): Int64 {
    var b = Boo()
    b.m = 1
    return b.m
}

class Boo {
    public mut prop m: Int64 {
        get() {
            return m
        }
        set(input) {
            var a = 0;
            m /= a
        }
    }
}
