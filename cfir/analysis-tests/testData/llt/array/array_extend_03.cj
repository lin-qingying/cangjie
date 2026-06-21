

// Test for reference access of size from extend.
extend<T> Array<T> {
    func returnSize(): Int64 {
        size
    }
}

main(): Int32 {
    let arr: Array<Int32> = [1]
    if (arr.returnSize() != 1) {
        return 1
    }
    return 0
}
