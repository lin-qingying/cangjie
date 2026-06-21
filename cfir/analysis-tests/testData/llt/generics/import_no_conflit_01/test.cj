// FILE: pkg.cj


package pkg

func foo<T>(fuzz_Func: (String) -> Unit) {
    return
}

class Test {
    func foo<T>(fuzz_Func: (String) -> Unit) {
        return
    }
}

// FILE: test.cj



internal import pkg.*

main() {
    0
}
