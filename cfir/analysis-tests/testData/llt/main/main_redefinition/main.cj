// FILE: pkg/pkg.cj


package pkg

main() {
    return 0
}

public func PrintHello() {
    println("Hello world?")
}

// FILE: main.cj



internal import pkg.*

main() {
    PrintHello()
    return 0
}
