

main(): Int64 {
    let arr1 = Array<Int64>(3) {i => i}
    if (arr1[2] != 2) {
        return 1
    }

    let arr2 = Array(3) {i: Int64 => Rune(48 + UInt32(i))}
    if (arr2[2] != r'2') {
        return 2
    }
    return 0
}
