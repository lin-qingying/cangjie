// RUN_PIPELINE_TILL: FRONTEND
interface HiddenMembers {
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>private<!> func hidden(): Unit {}
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>internal<!> func shared(): Unit {}
}
