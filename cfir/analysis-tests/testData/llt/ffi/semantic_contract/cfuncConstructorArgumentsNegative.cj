// 官方 SynCFuncCall 对零参和多参使用同一专用诊断；非指针实参另行诊断。
main() {
    let missingValue = <!CFUNC_TOO_MANY_ARGUMENTS!>CFunc<() -> Unit><!>()
    let wrongValue = CFunc<() -> Unit>(<!CFUNC_CTOR_MUST_BE_CPOINTER!>1<!>)
    let extraValue = <!CFUNC_TOO_MANY_ARGUMENTS!>CFunc<() -> Unit><!>(CPointer<Unit>(), CPointer<Unit>())
    0
}
