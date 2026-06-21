


func malloc<T>(initial: Option<T>): Int64 {
    initial.isSome()
    0
}

main() {
    var a: Option<CString> = <!TYPE_MISMATCH!>CString(CPointer())<!>
    var b: Option<Array<Int64>> = Array<Int64>()
    malloc<CPointer<Byte>>(CPointer())
    malloc<CString>(<!TYPE_MISMATCH!>CString(CPointer())<!>)
    0
}
