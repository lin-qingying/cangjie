// RUN_PIPELINE_TILL: FRONTEND
interface HiddenMembers {
    <!DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>private<!> func hidden(): Unit {}
    <!DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>internal<!> func shared(): Unit {}
}
