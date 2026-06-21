class Box<T> {}
class Base<T> {}
interface I<T> {}

extend<T> Base<Box<T>> <: I<T> {}

main() {
    var i1: I<Int64> = Base<Box<Int64>>()
    var i2: I<Box<Int64>> = Base<Box<Int64>>()
    0
}
