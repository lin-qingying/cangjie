package diagnostics

extend Int64 {
    func doubled(): Int64 {
        return this * 2
    }
}

extend<T> CPointer<T> {
    func pointeeTag(): Int64 {
        return 1
    }
}

func usePrimitive(value: Int64): Int64 {
    return value.doubled()
}

func usePointer(pointer: CPointer<Int64>): Int64 {
    return pointer.pointeeTag()
}

func useMissingMember(value: Int64): Unit {
    value.bogus()
}
