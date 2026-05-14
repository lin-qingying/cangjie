// RUN_PIPELINE_TILL: FRONTEND
interface MutCarrier {
    mut prop state: Int64 {
        get() {
            1
        }
        set(value) {
            let _ = value
        }
    }
}

interface MutParent <: MutCarrier {}

struct Record {}
class bb{

}
enum Choice {
    | A
}

extend bb <:  MutParent  {}
extend Record <:  MutParent  {}
extend Choice <: <!EXTEND_IMMUTABLE_MUT_INTERFACE!>MutCarrier<!> {}
