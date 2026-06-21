


interface A<U> where U <: A<U> {}

extend<U> <!ILLEGAL_EXTENDED_TYPE!>A<U><!> {}

main() {}
