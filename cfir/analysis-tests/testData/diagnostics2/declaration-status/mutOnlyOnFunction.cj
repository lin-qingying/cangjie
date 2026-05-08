// RUN_PIPELINE_TILL: FRONTEND
<!WRONG_MODIFIER_TARGET!>mut<!> class MutClass {}

class MutHolder {
    <!WRONG_MODIFIER_TARGET!>mut<!> var value: Int64 = 0
    <!WRONG_MODIFIER_TARGET!>mut<!> let fixed: Int64 = 1
    mut prop state: Int64 { get(){1} set(value){ let _ = value } }
    <!WRONG_MODIFIER_TARGET!>mut<!> func <!MUT_ONLY_ON_FUNCTION!>denied<!>(): Unit {}
}

struct MutStructHolder {
    mut prop state: Int64 {
        get() {
            1
        }
        set(value) {
            let _ = value
        }
    }
    mut func allowed(): Unit {}
}

<!WRONG_MODIFIER_TARGET!>mut<!> struct MutStruct {}

<!WRONG_MODIFIER_TARGET!>mut<!> enum MutEnum {
    | A
}
