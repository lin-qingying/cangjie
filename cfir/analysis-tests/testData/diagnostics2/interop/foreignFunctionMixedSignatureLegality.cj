// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
foreign func ok(value: CType): CType

foreign func badReturn(value: CType): <!INVALID_CFUNC_RETURN_TYPE!>String<!>

foreign func badParam(value: <!INVALID_CFUNC_PARAMETER_TYPE!>/* suggested message: foreign function parameter type must be C-compatible. source: external/cangjie_compiler/src/Sema/FFI/FFICheck.cpp */String<!>): CType

foreign func badBoth(
    first: <!INVALID_CFUNC_PARAMETER_TYPE!>/* suggested message: foreign function parameter type must be C-compatible. source: external/cangjie_compiler/src/Sema/FFI/FFICheck.cpp */(Int64, Int64)<!>,
    second: CType,
): <!INVALID_CFUNC_RETURN_TYPE!>String<!>
