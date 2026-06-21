// FILE: p1.cj


package p1

public class Foo {}

public interface I {}

// FILE: main.cj



internal import p1.Foo
internal import p1.I

public interface I1 {
    func f(): Unit {}
}

extend Foo <: I1 {}

main() {
    0
}
