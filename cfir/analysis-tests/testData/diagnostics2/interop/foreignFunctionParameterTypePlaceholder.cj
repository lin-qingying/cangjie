// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
foreign func takesCType(value: CType): Unit

foreign func takesString(value: <!INVALID_CFUNC_PARAMETER_TYPE!>/* suggested message: foreign function parameter type must be C-compatible. source: manual_source_zh_cn_cangjie_c_仓颉-C_互操作_0; external/cangjie_compiler/src/Sema/FFI/FFICheck.cpp */String<!>): Unit

foreign func takesTuple(value: <!INVALID_CFUNC_PARAMETER_TYPE!>/* suggested message: foreign function parameter type must be C-compatible. source: manual_source_zh_cn_cangjie_c_仓颉-C_互操作_0; external/cangjie_compiler/src/Sema/FFI/FFICheck.cpp */(Int64, Int64)<!>): Unit
