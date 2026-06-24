


interface Foo<T> {}

extend Int64 <: <!GENERIC_TYPE_SHOULD_BE_USED_WITH_TYPE_ARGUMENT!>Foo<!> {}
