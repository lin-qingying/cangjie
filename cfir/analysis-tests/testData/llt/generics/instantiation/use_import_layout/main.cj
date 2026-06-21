// FILE: pkg.cj


package pkg

public class C {}

extend C {
    public func foo(b!: Range<UInt16> = 0..10): Bool {
        return true
    }
}

// FILE: main.cj



internal import pkg.*

main() {
    var obj = C()
    obj.foo()
    0
}
