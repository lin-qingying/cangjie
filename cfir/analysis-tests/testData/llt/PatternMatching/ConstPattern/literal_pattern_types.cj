func floatOnInt(value: Int32): Unit {
    match (value) { case <!CANNOT_CONVERT_LITERAL!>1.1<!> => (); case _ => () }
}
func intOnFloat(value: Float32): Unit {
    match (value) { case <!CANNOT_CONVERT_LITERAL!>0<!> => (); case _ => () }
}
func intOnString(value: String): Unit {
    match (value) { case <!CANNOT_CONVERT_LITERAL!>50<!> => (); case _ => () }
}
func intOnUnit(value: Unit): Unit {
    match (value) { case <!CANNOT_CONVERT_LITERAL!>0<!> => (); case _ => () }
}
func boolOnInt(value: Int32): Unit {
    match (value) { case <!CANNOT_CONVERT_LITERAL!>true<!> => (); case _ => () }
}
func runeOnByte(value: UInt8): Unit {
    match (value) { case <!CANNOT_CONVERT_LITERAL!>r'A'<!> => (); case _ => () }
}
func intOnOption(value: Option<Int32>): Unit {
    match (value) { case <!TYPE_MISMATCH!>1<!> => (); case _ => () }
}
func intOnAny(value: Any): Unit {
    match (value) { case <!TYPE_MISMATCH!>1<!> => (); case _ => () }
}
func intOnInterface(value: ToString): Unit {
    match (value) { case <!TYPE_MISMATCH!>1<!> => (); case _ => () }
}
func intOnParameter<T>(value: T): Unit {
    match (value) { case <!CANNOT_CONVERT_LITERAL!>1<!> => (); case _ => () }
}
func nestedOption(value: Option<Option<Int32>>): Unit {
    match (value) { case <!TYPE_MISMATCH!>1<!> => (); case _ => () }
}
func suffixInt(value: Int32): Unit {
    match (value) { case <!CANNOT_CONVERT_LITERAL!>1i64<!> => (); case _ => () }
}
func suffixFloat(value: Float32): Unit {
    match (value) { case <!CANNOT_CONVERT_LITERAL!>1.0f64<!> => (); case _ => () }
}
func byteOnInt(value: Int32): Unit {
    match (value) { case <!CANNOT_CONVERT_LITERAL!>b'A'<!> => (); case _ => () }
}
func largeCharacter(value: UInt8): Unit {
    match (value) { case <!TYPE_MISMATCH!>'仓'<!> => (); case _ => () }
}
func tupleFailure(value: (String, Int64)): Unit {
    match (value) {
        case (<!CANNOT_CONVERT_LITERAL!>1<!>, a) => print(<!UNRESOLVED_REFERENCE!>a<!>)
        case _ => ()
    }
}
func tupleTypedFailure(value: (String, Int64)): Unit {
    match (value) {
        case (<!CANNOT_CONVERT_LITERAL!>1<!>, a: Int64) => print(a)
        case _ => ()
    }
}
func ifletFailure(value: (String, Int64)): Unit {
    if (let (<!CANNOT_CONVERT_LITERAL!>1<!>, a) <- value) { print(<!UNRESOLVED_REFERENCE!>a<!>) }
}
func whileletFailure(value: (String, Int64)): Unit {
    while (let (<!CANNOT_CONVERT_LITERAL!>1<!>, a) <- value) { print(<!UNRESOLVED_REFERENCE!>a<!>); break }
}
func integerRange(value: UInt8): Unit {
    match (value) {
        case <!LITERAL_NUMERIC_OVERFLOW!>-1<!> => ()
        case <!LITERAL_NUMERIC_OVERFLOW!>256<!> => ()
        case _ => ()
    }
}
func validIntegers(value: Int32): Unit {
    match (value) { case -1i32 => (); case 1 => (); case _ => () }
}
func validByte(value: UInt8): Unit {
    match (value) { case b'A' => (); case _ => () }
}
func characterByte(value: UInt8): Unit {
    match (value) { case 'é' => (); case _ => () }
}
func characterRune(value: Rune): Unit {
    match (value) { case '仓' => (); case _ => () }
}

func wrongSize(value: (UInt8, UInt8)): Unit {
    match (value) {
        case <!TUPLE_PATTERN_WITH_CORRECT_SIZE_EXPECTED!>(256, a, b)<!> => print(<!UNRESOLVED_REFERENCE!>a<!>)
    }
}
func wrongFirst(value: (String, UInt8)): Unit {
    match (value) { case (<!CANNOT_CONVERT_LITERAL!>1<!>, 256) => (); case _ => () }
}
func wrongTuple(value: UInt8): Unit {
    if (let <!TYPE_MISMATCH!>(256, a)<!> <- value) { print(<!UNRESOLVED_REFERENCE!>a<!>) }
}
