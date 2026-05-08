// RUN_PIPELINE_TILL: FRONTEND
foreign func takesCType(value: CType): Unit

foreign func takesString(value: <!INVALID_CFUNC_PARAMETER_TYPE!>String<!>): Unit

foreign func takesTuple(value: <!INVALID_CFUNC_PARAMETER_TYPE!>(Int64, Int64)<!>): Unit
