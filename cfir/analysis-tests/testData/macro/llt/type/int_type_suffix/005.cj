


interface I {}

extend<T> Array<T> <: I {}

main() {
    var a: I = "abc".toArray() // Array<UInt8>
    return 0
}
