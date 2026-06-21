

interface I {}

interface I1<T> {}

enum E <: I {
    EE
}

enum E1<T> <: I1<Int32> {
    EE1
}

enum E2<T> <: I1<T> {
    EE2
}

main(): Int64 {
    let a: I = EE
    let b: I1<Int32> = EE1<Int32>
    let c: I1<Rune> = EE2
    if (a is E && b is E1<Int32> && c is E2<Rune>) {
        return 0
    }
    return 1
}
