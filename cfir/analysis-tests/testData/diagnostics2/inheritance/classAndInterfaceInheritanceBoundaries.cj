// RUN_PIPELINE_TILL: FRONTEND
open class OpenBase {}
class FinalBase {}
interface Marker {}

class OpenChild <: OpenBase {}

class FinalChild <: <!CLASS_NOT_OPEN_FOR_INHERITANCE!>FinalBase<!> {}

<!INTERFACE_CANNOT_INHERIT_CLASS!>interface BadInterface<!> <: FinalBase {}

<!INTERFACE_CANNOT_INHERIT_CLASS!>interface MixedInterface<!> <: Marker & FinalBase {}
