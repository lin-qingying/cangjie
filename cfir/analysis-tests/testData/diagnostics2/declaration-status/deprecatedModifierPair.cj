// RUN_PIPELINE_TILL: FRONTEND
abstract <!REDUNDANT_MODIFIER!>open<!> class AbstractOpen {}

sealed <!REDUNDANT_MODIFIER!>public<!> class <!NON_ABSTRACT_CLASS_CANNOT_BE_SEALED!>PublicSealed<!> {}

sealed <!REDUNDANT_MODIFIER!>open<!> class <!NON_ABSTRACT_CLASS_CANNOT_BE_SEALED!>OpenSealed<!> {}
