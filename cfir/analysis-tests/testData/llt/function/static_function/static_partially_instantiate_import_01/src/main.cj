// FILE: pkga/a.cj


package pkga

public class Boo<T> {
    public static func foo<V>(x: T, y: V) {
        return y
    }
}

func foo() {
    return Boo<Int32>.foo<Int64>(1, 0)
}

// FILE: main.cj


internal import pkga.Boo

main(): Int64 {
    if (Boo<Int32>.foo<Bool>(1, true)) {
        return 0
    }
    return 1
}
