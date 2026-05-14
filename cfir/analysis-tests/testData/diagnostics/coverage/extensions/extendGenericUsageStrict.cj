// RUN_PIPELINE_TILL: FRONTEND
class Host {}
interface Display<T> {}

extend<T> <!EXTEND_GENERIC_USAGE!>Host<!> <: Display<T> {}
