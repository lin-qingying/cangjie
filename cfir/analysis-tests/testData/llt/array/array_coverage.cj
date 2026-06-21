

main() {
    var arr: Array<Int64> = [3, 45, 1, 65, 1, 2]
    func test(a!: Int64 = arr[0]) {
        return a
    }
    if (test() != 3) {
        return 1
    }
    return 0
}
