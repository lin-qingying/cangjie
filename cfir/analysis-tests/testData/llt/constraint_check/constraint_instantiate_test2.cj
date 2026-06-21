


open class A<T> where T <: A<T> {}

class AA <: A<AA> {}

main(): Int64 {
    return 0
}
