

func test(a: (Int64) -> Int32) {}

main() {
    let _ = Array<Int32>(5, <!TYPE_MISMATCH!>{=>}<!>)

    test(<!TYPE_MISMATCH!>{=>}<!>)
}
