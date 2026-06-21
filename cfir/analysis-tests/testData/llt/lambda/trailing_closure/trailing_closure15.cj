

func test(a: (Int64) -> Int64): (Int64) -> Int64 {
    return a
}

main() {
    let x = test <!TYPE_MISMATCH!>{i: Int8 => test {i => i}}<!>
    return 0
}
