// RUN_PIPELINE_TILL: FRONTEND
type GoodFunc = CFunc<() -> <!INVALID_CFUNC_RETURN_TYPE!>CType<!>>
type BadReturnFunc = CFunc<() -> <!INVALID_CFUNC_RETURN_TYPE!>String<!>>
type BadParamFunc = CFunc<(<!INVALID_CFUNC_PARAMETER_TYPE!>String<!>) -> <!INVALID_CFUNC_RETURN_TYPE!>CType<!>>

foreign func takesGood(fn: GoodFunc): Unit

foreign func takesBadReturn(
    fn: BadReturnFunc,
): Unit

foreign func takesBadParam(
    fn: BadParamFunc,
): Unit
