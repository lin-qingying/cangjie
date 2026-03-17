func literalNumericOverflow() {
    let a: Int8 = <!LITERAL_NUMERIC_OVERFLOW!>128i8<!>
    let b: UInt8 = <!LITERAL_NUMERIC_OVERFLOW!>256u8<!>
    let c: Int64 = <!LITERAL_NUMERIC_OVERFLOW!>9223372036854775808<!>
}
