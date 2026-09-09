

main(): Int64 {
    let a: Int64 = <!CANNOT_CONVERT_LITERAL!>1.2<!>
    let b: Int64 = <!CANNOT_CONVERT_LITERAL!>r'c'<!>
    let c: Int64 = <!CANNOT_CONVERT_LITERAL!>true<!>
    let d: Bool = <!CANNOT_CONVERT_LITERAL!>1<!>
    let e: Float64 = <!CANNOT_CONVERT_LITERAL!>2<!>
    let f: Rune = <!CANNOT_CONVERT_LITERAL!>3<!>
    return 1
}
