// 官方 TypeCheckBuiltinExpr.cpp：指针与 CFunc 转换保持各自 wrapper 类型及别名展开。
type Callback = CFunc<(Int64) -> Unit>
type BytePointer = CPointer<UInt8>

unsafe main() {
    let address = CPointer<Unit>()
    let callback: Callback = CFunc<(Int64) -> Unit>(address)
    let copied: CPointer<Unit> = CPointer<Unit>(callback)
    let bytes: BytePointer = CPointer<UInt8>(copied)
    let string = CString(bytes)
    let throughAlias = Callback(address)
    callback(1)
    throughAlias(2)
    0
}
