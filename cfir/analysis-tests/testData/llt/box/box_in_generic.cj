

interface I<T> {}

struct R<B> <: I<B> {}

struct F<X> {
    func f(): I<Int64> {
        R()
    }
}

main() {
    F<Int8>()
    return 0
}
