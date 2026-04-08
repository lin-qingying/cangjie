// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
type CTypeAlias = CType
type StringAlias = String
type A = StringAlias
type PairAlias = (Int64, Int64)

foreign func aliasOk(value: CTypeAlias): CTypeAlias

foreign func badAliasParam(value: <!INVALID_CFUNC_PARAMETER_TYPE!>/* suggested message: foreign function parameter type must remain C-compatible after typealias expansion. source: external/cangjie_compiler/src/Sema/FFI/FFICheck.cpp */StringAlias<!>): Unit

foreign func badAliasReturn(): <!INVALID_CFUNC_RETURN_TYPE!>A<!>

foreign func badTupleAlias(value: <!INVALID_CFUNC_PARAMETER_TYPE!>/* suggested message: foreign function parameter type must remain C-compatible after typealias expansion. source: external/cangjie_compiler/src/Sema/FFI/FFICheck.cpp */PairAlias<!>): Unit
