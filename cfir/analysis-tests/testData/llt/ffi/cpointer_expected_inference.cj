interface Marker {}
interface TypedMarker<T> {}
extend Int32 <: Marker {}
extend<T> CPointer<T> <: Marker where T <: Marker {}
extend<T> CPointer<T> <: TypedMarker<T> {}

let direct: CPointer<Int32> = CPointer()
let boxed: ?CPointer<Int32> = CPointer()
let erased: Marker = <!UNABLE_TO_INFER_GENERIC_FUNC!>CPointer<!>()
let erasedCast: Marker = <!UNABLE_TO_INFER_GENERIC_FUNC!>CPointer<!>(CPointer<Int64>())
let typed: TypedMarker<Int32> = CPointer()
let typedCast: TypedMarker<Int32> = CPointer(CPointer<Int64>())

main() { 0 }
