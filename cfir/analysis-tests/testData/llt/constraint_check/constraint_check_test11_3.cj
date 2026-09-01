

interface I {}

class D<T> {}

func bar<<!MULTIPLE_CLASS_UPPER_BOUNDS!>T<!>, U>() where T <: D<U> & D<I>, U <: I {}

main(): Int64 {
    return 1
}
