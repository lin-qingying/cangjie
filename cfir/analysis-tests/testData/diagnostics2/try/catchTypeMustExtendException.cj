// RUN_PIPELINE_TILL: FRONTEND
class NotException {}

func wrongCatchType(): Unit {
    try {
        throw Exception()
    }
    catch (e: <!CATCH_TYPE_MUST_EXTEND_EXCEPTION!>NotException<!>) {
        let _ = e
    }
}
