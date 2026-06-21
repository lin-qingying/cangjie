// FILE: pkg.cj


package pkg

public func TestFn1<T>(): Int64 {
    var x = 0
    func TestFn2(): Unit {
        x = 2
    }
    TestFn2()
    x
}

// FILE: main.cj



internal import pkg.*

main() {
    let v = TestFn1<Int64>()
    if (v != 2) {
        return 1
    }
    return 0
}
