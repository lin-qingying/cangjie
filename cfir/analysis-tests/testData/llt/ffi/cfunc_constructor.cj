// CFunc 的类型参数属于 wrapper 类型，构造参数必须是 CPointer。
unsafe main() {
    let callback: CFunc<(Int64) -> Unit> = CFunc<(Int64) -> Unit>(CPointer<Unit>())
    callback(1)
    0
}
