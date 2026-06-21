// FILE: pkg.cj


package pkg

func nextId() {
    12
}

public class M<T> {
    let id = nextId()
    public func f() {
        id
    }
}

// FILE: test.cj



internal import pkg.*

main() {
    println(M<String>().f())
}
