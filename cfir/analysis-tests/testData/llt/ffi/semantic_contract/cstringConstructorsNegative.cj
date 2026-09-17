// CString 只接受一个 CPointer<UInt8>；数量错误定位到参数表，类型错误定位到实参。
main() {
    let missingValue = CString<!WRONG_NUMBER_OF_ARGUMENTS!>()<!>
    let wrongPointer = CString(<!TYPE_MISMATCH!>CPointer<Int64>()<!>)
    let wrongValue = CString(<!CANNOT_CONVERT_LITERAL!>1<!>)
    let extraValue = CString<!WRONG_NUMBER_OF_ARGUMENTS!>(CPointer<UInt8>(), CPointer<UInt8>())<!>
    0
}
