

main(): Int64 {
    A<Int64>().a
    return 0
}

class A<T> {}

extend A<Int32> {}

extend A<Int64> {
    public prop a: Int64 {
        get() {
            return 1
        }
    }
}
