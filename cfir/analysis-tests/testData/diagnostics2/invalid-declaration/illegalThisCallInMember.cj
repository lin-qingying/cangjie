// RUN_PIPELINE_TILL: FRONTEND
class IllegalThisInMember {
    func build(): Unit {
        <!ILLEGAL_THIS_OR_SUPER_CALL!>this<!>()
    }
}
