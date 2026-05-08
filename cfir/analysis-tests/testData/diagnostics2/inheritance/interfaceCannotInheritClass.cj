// RUN_PIPELINE_TILL: FRONTEND
open class ConcreteBase {}
interface IMarker {}

<!INTERFACE_CANNOT_INHERIT_CLASS!>interface BadInterface<!> <: ConcreteBase {}

<!INTERFACE_CANNOT_INHERIT_CLASS!>interface MixedInterface<!> <: IMarker & ConcreteBase {}
