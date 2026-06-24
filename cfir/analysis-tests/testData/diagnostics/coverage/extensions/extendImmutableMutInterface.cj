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
<!EXTEND_INTERFACE_NOT_EXTENDABLE!>extend Choice <: MutCarrier {}<!>
