func int32Value(): Int32 {
    return 4
}

struct S {
    operator func +(_: Int64) {
        S()
    }
}

main() {
    println(int32Value() - 4)
    let result32: Int32 = int32Value() - 4
    let result64: Int64 = int32Value() - 4
    println(Int32(4) - Int64(4))
    println(Int32(4) + 4)
    (S() + 1)
    0
}
