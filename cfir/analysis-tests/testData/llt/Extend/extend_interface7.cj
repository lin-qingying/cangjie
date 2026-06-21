


class A {}

extend Int64 <: <!EXTEND_NOT_INTERFACE!>Int64<!> {}

extend A <: <!EXTEND_NOT_INTERFACE!>Int64<!> {}

extend Int64 <: <!EXTEND_NOT_INTERFACE!>A<!> {}

extend A <: <!EXTEND_NOT_INTERFACE!>A<!> {}

extend A <: <!EXTEND_NOT_INTERFACE!>Int64<!> & <!EXTEND_NOT_INTERFACE!>A<!> {}
