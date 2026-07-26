// FILE: pkg/pkg.cj


package pkg

public interface A<T> {
    func foo(a: T): T {
        return a
    }
}

// FILE: class_no_override_modifier_invalid_9.cj



internal import pkg.*

interface B<T> {
    func foo(b: T): T {
        return b
    }
}

class <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>D<!><T> <: A<T> & B<T> {}

main() {
    return 1
}
