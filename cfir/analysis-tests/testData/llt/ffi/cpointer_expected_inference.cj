interface Marker {}
interface TypedMarker<T> {}
extend Int32 <: Marker {}
extend<T> CPointer<T> <: Marker where T <: Marker {}
extend<T> CPointer<T> <: TypedMarker<T> {}

let direct: CPointer<Int32> = CPointer()
let boxed: ?CPointer<Int32> = CPointer()
let erased: Marker = <!POINTER_UNKNOW_GENERIC_TYPE!>CPointer<!>()
let erasedCast: Marker = <!POINTER_UNKNOW_GENERIC_TYPE!>CPointer<!>(CPointer<Int64>())
let typed: TypedMarker<Int32> = CPointer()
let typedCast: TypedMarker<Int32> = CPointer(CPointer<Int64>())

main() { 0 }
