// RUN_PIPELINE_TILL: FRONTEND
foreign func ok(value: CType): CType

foreign func badReturn(value: CType): <!INVALID_CFUNC_RETURN_TYPE!>String<!>

foreign func badParam(value: <!INVALID_CFUNC_PARAMETER_TYPE!>String<!>): CType

foreign func badBoth(
    first: <!INVALID_CFUNC_PARAMETER_TYPE!>(Int64, Int64)<!>,
    second: CType,
): <!INVALID_CFUNC_RETURN_TYPE!>String<!>
