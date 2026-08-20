// RUN_PIPELINE_TILL: FRONTEND
open class BaseA {}
open class BaseB {}
open class BaseC {}
interface Marker {}

class ChildOne <: BaseA & <!ILLEGAL_MULTI_INHERITANCE!>BaseB<!> & Marker {}

class ChildTwo <: BaseA & <!ILLEGAL_MULTI_INHERITANCE!>BaseB<!> & <!ILLEGAL_MULTI_INHERITANCE!>BaseC<!> {}
