// FILE: pkg1/pkg1.cj


package pkg1

extend Int64 {
    public prop p: Int64 {
        get() {
            0
        }
    }
}

public interface I {
    func f() {
        1.p
    }
}

// FILE: pkg2/pkg2.cj


package pkg2

internal import pkg1.*

// FILE: main.cj


internal import pkg2.*

main(): Int64 {
    return 0
}
