foreign func nativeFunction(value: Int64): Int64

@C
func cFunction(value: Int64): Int64 {
    value
}

main() {
    <!UNSAFE_FUNCTION_INVOKE_FAILED!>nativeFunction(1)<!>
    <!UNSAFE_FUNCTION_INVOKE_FAILED!>cFunction(1)<!>
    let callback: CFunc<(Int64) -> Int64> = { value => value }
    <!UNSAFE_FUNCTION_INVOKE_FAILED!>callback(1)<!>

    unsafe {
        nativeFunction(1)
        cFunction(1)
        callback(1)
    }
    0
}
