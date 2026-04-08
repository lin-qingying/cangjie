// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
@CallingConv[CDECL]
foreign func cdeclOkNoArgs(): Unit

class Holder {
    <!INVALID_CALLING_CONVENTION_TARGET!>/* suggested message: @CallingConv cannot decorate member declarations that are not valid foreign interop entrypoints. source: analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/components/CaCfirInteropProtocol.kt */@CallingConv[CDECL]
    func memberBad(): Unit {}<!>
}
