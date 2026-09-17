// 指针构造的类型实参约束、值参数类型和参数数目分别由官方对应 owner 诊断。
main() {
    let invalidElement = CPointer<<!GENERIC_TYPE_ARGUMENT_NOT_MATCH_CONSTRAINT!>String<!>>()
    let wrongValue = <!POINTER_SINGLE_ELEMENT_TYPE_ERROR!>CPointer<Unit>(1)<!>
    let extraValue = <!POINTER_TOO_MUCH_ARGUMENT!>CPointer<Unit>(CPointer<Unit>(), CPointer<Unit>())<!>
    0
}
