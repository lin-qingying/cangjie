


type C<T> = CPointer<T>

var node4_2: <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>C<!> = C<Int64>() // error.

type D = C<Int64>

var node5_2: <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>C<!> = D() // error.

foreign func malloc(size: UIntNative): CPointer<Unit>

var node17 = <!POINTER_UNKNOW_GENERIC_TYPE!>CPointer<!>(malloc) // error.
var node8: <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>CPointer<!> = CPointer<Unit>() // error.
var node9: CType = <!POINTER_UNKNOW_GENERIC_TYPE!>CPointer<!>() // error.
var node14: CString = <!POINTER_UNKNOW_GENERIC_TYPE!>CPointer<!>() // error.
var node15 = <!POINTER_UNKNOW_GENERIC_TYPE!>CPointer<!>() // error.
var node16: <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>CPointer<!> = <!POINTER_UNKNOW_GENERIC_TYPE!>CPointer<!>() // error.

var node13 = <!POINTER_UNKNOW_GENERIC_TYPE!>CPointer<!>(CPointer<Unit>()) // error.

main() {}
