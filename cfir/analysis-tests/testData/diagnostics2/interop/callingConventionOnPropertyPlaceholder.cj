// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
class Holder {
    <!INVALID_CALLING_CONVENTION_TARGET!>/* suggested message: @CallingConv cannot decorate stored properties or non-callable declarations. source: analysis/analysis-api-cfir/src/org/cangnova/cangjie/analysis/api/cfir/components/CaCfirInteropProtocol.kt */@CallingConv[CDECL]
    let prop = 1<!>
}
