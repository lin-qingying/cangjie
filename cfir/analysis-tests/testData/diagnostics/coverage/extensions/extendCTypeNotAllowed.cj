// RUN_PIPELINE_TILL: FRONTEND
@C
class NativeBox {}

@Java
class JavaBox {}

interface Printable {}

extend <!EXTEND_C_TYPE_NOT_ALLOWED!>NativeBox<!> <: Printable {}
extend <!EXTEND_C_TYPE_NOT_ALLOWED!>JavaBox<!> <: Printable {}
