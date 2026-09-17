// RUN_PIPELINE_TILL: FRONTEND
@CallingConv[CDECL]
foreign func cdeclOk(value: <!INVALID_CFUNC_PARAMETER_TYPE!>CType<!>): <!INVALID_CFUNC_RETURN_TYPE!>CType<!>

@CallingConv[STDCALL]
foreign func stdcallOk(value: <!INVALID_CFUNC_PARAMETER_TYPE!>CType<!>): <!INVALID_CFUNC_RETURN_TYPE!>CType<!>

// Official CFFICheck.cpp currently accepts only CDECL and STDCALL here.

<!ONLY_CFUNC_CAN_USE_ANNOTATION!>@CallingConv[CDECL]<!>
func nonForeignBad(): Unit {}
