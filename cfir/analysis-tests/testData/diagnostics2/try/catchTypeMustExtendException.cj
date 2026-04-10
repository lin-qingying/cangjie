// RUN_PIPELINE_TILL: FRONTEND
// IGNORE_ERRORS
class NotException {}

func wrongCatchType(): Unit {
    try {
        throw Exception()
    } <!CATCH_TYPE_MUST_EXTEND_EXCEPTION!>/* suggested message: the exception catch type must be class and extends from core.Exception or core.Error. source: cjc 1.0.1 actual output; DiagnosticSema.def:sema_except_catch_type_error; TypeCheckPattern.cpp:ChkExceptTypePattern checks both Exception and Error */catch (e: NotException)<!> {
        println(e)
    }
}
