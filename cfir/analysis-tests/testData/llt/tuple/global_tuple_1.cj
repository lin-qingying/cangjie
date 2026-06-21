

open class A {}

class B <: A {}

var tuple: (A, A, A) = (B(), B(), B())

main(): Int64 {
    return 0
}
