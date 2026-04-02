// RUN_PIPELINE_TILL: FRONTEND
<!REDUNDANT_MODIFIER_FOR_TARGET!>open<!> interface Marker {}

abstract <!REDUNDANT_MODIFIER!>open<!> class AbstractOpen

<!WRONG_MODIFIER_TARGET!>override<!> func topLevelOverride(): Unit {}

func outer(): Unit {
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>private<!> func localHidden(): Unit {}
}

public <!REPEATED_MODIFIER!>public<!> class DuplicateVisibility
