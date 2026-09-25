


interface I2 {}

extend Int32 <: I2 {}

extend Unit <: I2 {}

extend<T> CPointer<T> <: I2 where T <: I2 {}

var node19: I2 = <!POINTER_UNKNOW_GENERIC_TYPE!>CPointer<!>()
var node20: I2 = <!TYPE_MISMATCH!>CPointer<Int64>()<!>
var node21: I2 = <!POINTER_UNKNOW_GENERIC_TYPE!>CPointer<!>(CPointer<Int64>())

main() {}
