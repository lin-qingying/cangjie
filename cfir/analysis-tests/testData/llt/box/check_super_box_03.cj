

interface I<T> {}

open class A<T> <: I<T> {}

open class B <: A<Int32> {}

class C <: B {}

extend C <: I<Rune> {}

main() {
    let ins0: Any = C()
    let v1 = ins0 is B && ins0 is A<Int32> && ins0 is I<Int32>
    let ins1: I<Rune> = C()
    let v2 = ins1 is B && ins1 is A<Int32> && ins1 is I<Int32>
    if (!v1) {
        return 1
    } else if (!v2) {
        return 2
    }
    return 0
}
