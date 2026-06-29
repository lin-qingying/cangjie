// RUN_PIPELINE_TILL: FRONTEND
class IllegalThisInMember {
    func build(): Unit {
        <!INVALID_THIS_CALL_OUTSIDE_CTOR!>this<!>()
    }
}
