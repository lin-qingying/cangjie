// 命名参数规则不得取决于 synthetic 参数内部名字，且两种 builtin 的错误优先级不同。
main() {
    let pointerNamed = CPointer<Unit>(<!UNKNOWN_NAMED_ARGUMENT!>value<!>: CPointer<Unit>())
    let callbackNamed = CFunc<() -> Unit>(<!UNKNOWN_NAMED_ARGUMENT!>pointer<!>: CPointer<Unit>())
    unsafe { callbackNamed() }

    let pointerWrong = <!POINTER_SINGLE_ELEMENT_TYPE_ERROR!>CPointer<Unit>(value: 1)<!>
    let callbackWrong = CFunc<() -> Unit>(<!CFUNC_CTOR_MUST_BE_CPOINTER!><!UNKNOWN_NAMED_ARGUMENT!>value<!>: 1<!>)
    0
}
