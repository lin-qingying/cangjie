


main(): Int64 {
    let x: Int64
    let f = <!USE_FUNC_CAPTURE_VAR_ALONE!>{=> <!CAPTURE_BEFORE_INITIALIZATION!>x<!>}<!>
    var y: Int32
    <!USE_FUNC_CAPTURE_VAR_ALONE!>{a: Int32, b: Int32 => a + b + <!CAPTURE_BEFORE_INITIALIZATION!>y<!>}<!>
    return 0
}
