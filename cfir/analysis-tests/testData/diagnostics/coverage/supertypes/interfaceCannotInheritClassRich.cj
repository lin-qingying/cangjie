// RUN_PIPELINE_TILL: FRONTEND
open class ConcreteBase {}
interface IMarker {}

interface <!INTERFACE_CANNOT_INHERIT_CLASS!>BadInterface<!> <: ConcreteBase {}

interface <!INTERFACE_CANNOT_INHERIT_CLASS!>MixedInterface<!> <: IMarker & ConcreteBase {}
