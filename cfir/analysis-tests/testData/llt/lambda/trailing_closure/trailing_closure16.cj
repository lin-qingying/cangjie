

func test(a: (Int64) -> Int64, b: Int64): (Int64) -> Int64 {
    return a
}

main() {
    let x = <!NO_VALUE_FOR_PARAMETER!>test {i: Int8 => {i => i}}<!>
    return 0
}
