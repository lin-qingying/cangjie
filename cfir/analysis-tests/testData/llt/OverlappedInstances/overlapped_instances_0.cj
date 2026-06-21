


func f<X>(a: X) where X <: I<X> & I<Int64> {}

interface I<X> {}

extend Bool <: I<Bool> {}

extend Bool <: I<Int64> {}

main() {
    f(true)
    0
}
