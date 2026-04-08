// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
type GoodFunc = CFunc<() -> CType>
type BadReturnFunc = CFunc<() -> String>
type BadParamFunc = CFunc<(String) -> CType>

foreign func takesGood(fn: GoodFunc): Unit

foreign func takesBadReturn(
    fn: <!INVALID_CFUNC_RETURN_TYPE!>/* suggested message: CFunc return type must be C-compatible. source: docs/diagnostics-gap-vs-official-cpp-sema-detailed.md; external/cangjie_compiler/src/Sema/FFI/FFICheck.cpp */BadReturnFunc<!>,
): Unit

foreign func takesBadParam(
    fn: <!INVALID_CFUNC_PARAMETER_TYPE!>/* suggested message: CFunc parameter type must be C-compatible. source: docs/diagnostics-gap-vs-official-cpp-sema-detailed.md; external/cangjie_compiler/src/Sema/FFI/FFICheck.cpp */BadParamFunc<!>,
): Unit
