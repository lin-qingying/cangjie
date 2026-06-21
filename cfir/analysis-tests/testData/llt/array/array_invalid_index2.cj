


main(): Int64 {
    let arr = [2, 1, 0]
    arr[1..2] = arr[0..1]
    <!CANNOT_ASSIGN_TO_SUBSCRIPT!>arr[1..2]<!> += arr[0..1]
    return 0
}
