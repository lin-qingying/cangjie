

interface I<T> {
    operator func -(): T
}

extend<T> Array<T> {
    public func getOrElse(index: Int64, defaultValue: (Int64) -> T): T {
        return defaultValue(index)
    }
}

extend Int32 <: I<Int32> {}

main(): Int32 {
    let arr: Array<Int32> = [1]
    if (-arr[0] != -1) {
        return 1
    }
    return 0
}
