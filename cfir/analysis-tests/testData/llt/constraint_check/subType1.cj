


interface I<U> {}

open class Data<T> <: I<T> {}

class Data2<T> <: Data<T> {}

main() {
    var a: I<Int32> = Data<Int32>()
    var b: I<Int32> = Data2<Int32>()
    var c: Data<Int32> = Data2<Int32>()
    return 0
}
