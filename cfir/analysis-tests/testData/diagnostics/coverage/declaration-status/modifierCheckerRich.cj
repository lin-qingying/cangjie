// RUN_PIPELINE_TILL: FRONTEND
<!REDUNDANT_MODIFIER_FOR_TARGET!>open<!> interface Marker {
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>private<!> func hidden(): Unit {}
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>internal<!> func shared(): Unit {}
}

abstract <!REDUNDANT_MODIFIER!>open<!> class AbstractOpen {}

sealed <!REDUNDANT_MODIFIER!>public<!> class PublicSealed {}

sealed <!REDUNDANT_MODIFIER!>open<!> class OpenSealed {}

<!WRONG_MODIFIER_TARGET!>override<!> func topLevelOverride(): Unit {}

func outer(): Unit {
    <!WRONG_MODIFIER_CONTAINING_DECLARATION!>private<!> func localHidden(): Unit {}
}

public <!REPEATED_MODIFIER!>public<!> class DuplicateVisibility {}
