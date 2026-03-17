open class ConcreteBase {}
interface IMarker {}

interface BadInterface <: <!INTERFACE_CANNOT_INHERIT_CLASS!>ConcreteBase<!> {}

interface MixedInterface <: IMarker & <!INTERFACE_CANNOT_INHERIT_CLASS!>ConcreteBase<!> {}
