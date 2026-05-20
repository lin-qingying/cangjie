// RUN_PIPELINE_TILL: FRONTEND
@CallingConv[CDECL]
foreign func cdeclOk(value: CType): CType

@CallingConv[STDCALL]
foreign func stdcallOk(value: CType): CType

// Official CFFICheck.cpp currently accepts only CDECL and STDCALL here.

<!ONLY_CFUNC_CAN_USE_ANNOTATION!>@CallingConv[CDECL]<!>
func nonForeignBad(): Unit {}
