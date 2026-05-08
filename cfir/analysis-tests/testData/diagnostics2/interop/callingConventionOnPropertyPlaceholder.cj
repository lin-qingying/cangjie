// RUN_PIPELINE_TILL: FRONTEND
class Holder {
    <!ILLEGAL_SCOPE_USE_OF_ANNOTATION!>@CallingConv[CDECL]<!>
    let value = 1
}
