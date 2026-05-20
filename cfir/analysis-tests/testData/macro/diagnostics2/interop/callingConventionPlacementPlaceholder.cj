// RUN_PIPELINE_TILL: FRONTEND
@CallingConv[CDECL]
foreign func cdeclOkNoArgs(): Unit

class Holder {
    <!ILLEGAL_SCOPE_USE_OF_ANNOTATION!>@CallingConv[CDECL]<!>
    func memberBad(): Unit {}
}
