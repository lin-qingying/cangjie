// RUN_PIPELINE_TILL: FRONTEND
open class OpenBase {}
class FinalBase {}
interface Marker {}

class OpenChild <: OpenBase {}

class FinalChild <: <!CLASS_NOT_OPEN_FOR_INHERITANCE!>FinalBase<!> {}

interface BadInterface <: <!INTERFACE_CANNOT_INHERIT_CLASS!>FinalBase<!> {}

interface MixedInterface <: Marker & <!INTERFACE_CANNOT_INHERIT_CLASS!>FinalBase<!> {}
