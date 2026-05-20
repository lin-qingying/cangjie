// RUN_PIPELINE_TILL: FRONTEND
foreign func ok(): CType

foreign func badString(): <!INVALID_CFUNC_RETURN_TYPE!>String<!>

foreign func badTuple(): <!INVALID_CFUNC_RETURN_TYPE!>(Int64, Int64)<!>

type CTypeAlias = CType
foreign func aliasOk(): CTypeAlias
