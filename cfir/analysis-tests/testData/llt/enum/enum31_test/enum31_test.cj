// FILE: pkg.cj


package pkg

public enum ET {
    A | B | C

    public prop a: Int64 {
        get() {
            1
        }
    }
}

// FILE: enum31_test.cj



internal import pkg.*

main() {
    let obj = ET.A
    return obj.a - 1
}
