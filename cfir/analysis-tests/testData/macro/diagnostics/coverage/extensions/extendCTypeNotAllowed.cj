// RUN_PIPELINE_TILL: FRONTEND
<!ILLEGAL_USE_OF_ANNOTATION!>@C<!>
class NativeBox {}

@<!UNRESOLVED_REFERENCE!>Java<!>
class JavaBox {}

interface Printable {}

extend <!EXTEND_C_TYPE_NOT_ALLOWED!>NativeBox<!> <: Printable {}
extend JavaBox <: Printable {}
