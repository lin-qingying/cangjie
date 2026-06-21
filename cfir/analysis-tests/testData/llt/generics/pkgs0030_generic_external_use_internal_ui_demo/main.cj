// FILE: pkgs/pkgs.cj


package pkgs

public class B {}

public class A<T> {
    public var a: B = B()
}

// FILE: main.cj



internal import pkgs.*

main() {
    let arg = Option<Int32>.Some(12)
    let a = A<Int32>()
    return 0
}
