// RUN_PIPELINE_TILL: FRONTEND
open class BaseA {}
open class BaseB {}
open class BaseC {}
interface Marker {}

class ChildOne <: <!MULTIPLE_CLASS_SUPER_TYPES!>BaseA<!> & BaseB & Marker {}

class ChildTwo <: <!MULTIPLE_CLASS_SUPER_TYPES!>BaseA<!> & BaseB & BaseC {}
