// RUN_PIPELINE_TILL: FRONTEND
open class BaseA {}
open class BaseB {}
open class BaseC {}
interface Marker {}

class ChildOne <: BaseA & <!MULTIPLE_CLASS_SUPER_TYPES!>BaseB<!> & Marker {}

class ChildTwo <: BaseA & <!MULTIPLE_CLASS_SUPER_TYPES!>BaseB<!> & <!MULTIPLE_CLASS_SUPER_TYPES!>BaseC<!> {}
