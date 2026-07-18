

main(): Int64 {
    let a: Int64 = <!TYPE_MISMATCH!>1.2<!>
    let b: Int64 = <!CANNOT_CONVERT_LITERAL!>r'c'<!>
    let c: Int64 = <!TYPE_MISMATCH!>true<!>
    let d: Bool = <!TYPE_MISMATCH!>1<!>
    let e: Float64 = <!TYPE_MISMATCH!>2<!>
    let f: Rune = <!TYPE_MISMATCH!>3<!>
    return 1
}
