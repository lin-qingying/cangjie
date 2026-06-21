// FILE: pkg.cj


package pkg

public class A<T> {
    public static let EOF = -1
}

public func test() {
    let a = A<Int64>()
}

// FILE: main.cj



// Test for correct import static let var EOF from instantiated decl
internal import pkg.*

main() {
    let x = A<Rune>.EOF
}
