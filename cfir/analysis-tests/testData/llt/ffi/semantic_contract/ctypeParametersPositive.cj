// 官方基线：896235c9fd18f22d570a9818ac672838c36c3932，CFFICheck.cpp / Ty::IsMetCType。
@C
struct NativeRecord {
    var value: Int64 = 0
}

foreign func primitiveParameters(
    boolean: Bool,
    i8: Int8, i16: Int16, i32: Int32, i64: Int64, nativeInt: IntNative,
    u8: UInt8, u16: UInt16, u32: UInt32, u64: UInt64, nativeUInt: UIntNative,
    f32: Float32, f64: Float64
): Unit

foreign func mappedParameters(
    address: CPointer<Unit>,
    string: CString,
    callback: CFunc<(Int32) -> Unit>,
    record: NativeRecord
): CPointer<NativeRecord>

type NativeInteger = Int64
type NativeAddress = CPointer<NativeInteger>
type NativeCallback = CFunc<(NativeInteger) -> Unit>

foreign func aliases(address: NativeAddress, callback: NativeCallback): NativeInteger

main() { 0 }
