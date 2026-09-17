// RUN_PIPELINE_TILL: FRONTEND
foreign func ok(value: <!INVALID_CFUNC_PARAMETER_TYPE!>CType<!>): <!INVALID_CFUNC_RETURN_TYPE!>CType<!>

foreign func badReturn(value: <!INVALID_CFUNC_PARAMETER_TYPE!>CType<!>): <!INVALID_CFUNC_RETURN_TYPE!>String<!>

foreign func badParam(value: <!INVALID_CFUNC_PARAMETER_TYPE!>String<!>): <!INVALID_CFUNC_RETURN_TYPE!>CType<!>

foreign func badBoth(
    first: <!INVALID_CFUNC_PARAMETER_TYPE!>(Int64, Int64)<!>,
    second: <!INVALID_CFUNC_PARAMETER_TYPE!>CType<!>,
): <!INVALID_CFUNC_RETURN_TYPE!>String<!>
