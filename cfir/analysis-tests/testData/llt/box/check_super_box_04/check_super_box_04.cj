// FILE: pkg.cj


package pkg

public interface I<T> {}

public open class A<T> <: I<T> {}

public open class B <: A<Int32> {}

public class C <: B {}

extend C <: I<Rune> {}

public let v: Any = B()

// FILE: check_super_box_04.cj


internal import pkg.*

interface I1 {}

extend C <: I1 {}

main() {
    let ins0: Any = C()
    let v1 = ins0 is B && ins0 is A<Int32> && ins0 is I<Int32> && ins0 is I<Rune>

    let ins1: I<Rune> = C()
    let v2 = ins1 is B && ins1 is A<Int32> && ins1 is I<Int32> && ins1 is I<Rune>

    let ins2: I1 = C()
    let v3 = ins2 is B && ins2 is A<Int32> && ins2 is I<Int32> && ins2 is I<Rune>

    let v4 = v is B && v is A<Int32> && v is I<Int32>

    if (!v1) {
        return 1
    } else if (!v2) {
        return 2
    } else if (!v3) {
        return 3
    } else if (!v4) {
        return 4
    }
    return 0
}
