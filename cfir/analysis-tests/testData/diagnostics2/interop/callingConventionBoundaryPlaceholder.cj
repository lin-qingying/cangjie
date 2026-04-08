// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
@CallingConv[CDECL]
foreign func cdeclOk(value: CType): CType

@CallingConv[STDCALL]
foreign func stdcallOk(value: CType): CType

<!INVALID_CALLING_CONVENTION_TARGET!>/* suggested message: @CallingConv should only appear on valid interop declarations. source: analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/components/CaCfirInteropProtocol.kt; psi/src/org/cangnova/cangjie/parsing/CangJieParsing.kt */@CallingConv[CDECL]<!>
func nonForeignBad(): Unit {}
