// CType 接口本身、Option、Rune、Float16、String 和普通函数均不满足 C ABI 类型要求。
foreign func rejectString(value: <!INVALID_CFUNC_PARAMETER_TYPE!>String<!>): <!INVALID_CFUNC_RETURN_TYPE!>String<!>
foreign func rejectRune(value: <!INVALID_CFUNC_PARAMETER_TYPE!>Rune<!>): Unit
foreign func rejectFloat16(value: <!INVALID_CFUNC_PARAMETER_TYPE!>Float16<!>): Unit
foreign func rejectOption(value: <!INVALID_CFUNC_PARAMETER_TYPE!>?Int64<!>): Unit
foreign func rejectInterface(value: <!INVALID_CFUNC_PARAMETER_TYPE!>CType<!>): <!INVALID_CFUNC_RETURN_TYPE!>CType<!>
foreign func rejectFunction(value: <!INVALID_CFUNC_PARAMETER_TYPE!>(Int64) -> Unit<!>): Unit
foreign func rejectUnit(<!CFUNC_CANNOT_HAVE_UNIT_ARGS!>value: Unit<!>): Unit
foreign func rejectNamed(<!CFUNC_CANNOT_HAVE_NAMED_ARGS!>value!: Int64<!>): Unit
foreign func returnArray(): <!VARRAY_IN_CFUNC!>VArray<Int64, $2><!>

main() { 0 }
