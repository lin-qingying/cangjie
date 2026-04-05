// RUN_PIPELINE_TILL: FRONTEND
class Host {}
interface Display<T> {}

extend<<!EXTEND_GENERIC_USAGE!>T<!>> Host <: Display<T> {}
