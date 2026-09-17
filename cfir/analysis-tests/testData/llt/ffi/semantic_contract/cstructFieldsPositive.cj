// 官方 CType 递归允许 C struct、CFunc 和元素满足 CType 的 VArray 字段。
@C
struct NestedRecord {
    var value: Int32 = 0
}

@C
struct NativeRecord {
    var boolean: Bool = false
    var integer: Int64 = 0
    var number: Float64 = 0.0
    var address: CPointer<Unit> = CPointer<Unit>()
    var string: CString = CString(CPointer<UInt8>())
    var callback: CFunc<(Int32) -> Unit> = { value => () }
    var record: NestedRecord = NestedRecord()
    var values: VArray<UInt8, $2> = [0, 1]
}

main() { 0 }
