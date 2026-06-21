

main() {
    let arr0 = Array(3, repeat: 'c')
    let arr1 = Array<Int8>(3, repeat: 3)
    let arr2: Array<ToString> = Array(3, repeat: 3)
    if (arr0[2] != 'c' || arr1[2] != 3) {
        return 1
    }
    if (arr2[2].toString() != "3") {
        return 2
    }
    return 0
}
