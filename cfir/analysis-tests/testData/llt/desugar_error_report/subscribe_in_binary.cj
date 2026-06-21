


main(): Int64 {
    let b = [[1u8, 2u8, 3u8]]
    <!PARAMETERS_AND_ARGUMENTS_MISMATCH!>b<!>[<!CANNOT_CONVERT_LITERAL!>1<!>.01][0] != 1
    return 0
}
