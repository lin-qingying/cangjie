class ExtendHost {}
class PlainClass {}
interface Marker {}

extend ExtendHost <: <!EXTEND_NOT_INTERFACE!>PlainClass<!> & <!EXTEND_DUPLICATE_INTERFACE!>Marker<!> {}
extend ExtendHost <: Int64 & <!EXTEND_DUPLICATE_INTERFACE!>Marker<!> {}
