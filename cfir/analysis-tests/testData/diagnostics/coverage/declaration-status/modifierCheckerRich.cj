// RUN_PIPELINE_TILL: FRONTEND
<!DEPRECATED_MODIFIER_FOR_TARGET!>open<!> interface Marker {
    <!DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>private<!> func hidden(): Unit {}
    <!DEPRECATED_MODIFIER_CONTAINING_DECLARATION!>internal<!> func shared(): Unit {}
}

<!DEPRECATED_MODIFIER_PAIR!>abstract<!> <!DEPRECATED_MODIFIER_PAIR!>open<!> class AbstractOpen

<!DEPRECATED_MODIFIER_PAIR!>sealed<!> <!DEPRECATED_MODIFIER_PAIR!>public<!> class PublicSealed

<!DEPRECATED_MODIFIER_PAIR!>sealed<!> <!DEPRECATED_MODIFIER_PAIR!>open<!> class OpenSealed

<!WRONG_MODIFIER_TARGET!>override<!> func topLevelOverride(): Unit {}

func outer(): Unit {
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>private<!> func localHidden(): Unit {}
}

public <!REPEATED_MODIFIER!>public<!> class DuplicateVisibility
