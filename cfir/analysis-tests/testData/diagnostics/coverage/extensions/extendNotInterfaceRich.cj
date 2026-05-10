// RUN_PIPELINE_TILL: FRONTEND
class ExtendHost {}
class PlainClass {}
interface Marker {}

extend ExtendHost <: <!EXTEND_NOT_INTERFACE!>PlainClass<!> & Marker {}
extend ExtendHost <: <!EXTEND_NOT_INTERFACE!>Int64<!> & Marker {}
