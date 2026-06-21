


interface X<T> where T <: Y<T> {}

interface Y<T> where T <: X<T> {}

interface C <: X<C> & Y<C> {}

main() {
    return 0
}
