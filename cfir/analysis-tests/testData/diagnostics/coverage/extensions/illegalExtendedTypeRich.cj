interface IllegalTarget {}
interface AnotherInterface {}

extend <!ILLEGAL_EXTENDED_TYPE!>IllegalTarget<!> {}
extend <!ILLEGAL_EXTENDED_TYPE!>AnotherInterface<!> {}
