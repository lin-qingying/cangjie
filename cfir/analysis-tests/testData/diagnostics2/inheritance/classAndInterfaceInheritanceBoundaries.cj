// RUN_PIPELINE_TILL: FRONTEND
open class OpenBase {}
class FinalBase {}
interface Marker {}

class OpenChild <: OpenBase {}

class FinalChild <: <!CLASS_NOT_OPEN_FOR_INHERITANCE!>FinalBase<!> {}

interface <!INTERFACE_CANNOT_INHERIT_CLASS!>BadInterface<!> <: FinalBase {}

interface <!INTERFACE_CANNOT_INHERIT_CLASS!>MixedInterface<!> <: Marker & FinalBase {}
