// FILE: pkga/pkga.cj


package pkga

public class A {}

public func C2<T>(): Int32 {
    1
}

public func B<T>(): Int32 {
    return C2<Int32>() + C2<T>()
}

// FILE: main.cj



internal import pkga.*

func run(): Int64 {
    if (B<Int32>() != 2) {
        return 1
    }
    return 0
}

main(): Int64 {
    return run()
}
