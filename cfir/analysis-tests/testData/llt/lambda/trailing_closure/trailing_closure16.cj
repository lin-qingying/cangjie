

func test(a: (Int64) -> Int64, b: Int64): (Int64) -> Int64 {
    return a
}

main() {
    let x = <!WRONG_NUMBER_OF_ARGUMENTS!>test {i: Int8 => {i => i}}<!>
    return 0
}
