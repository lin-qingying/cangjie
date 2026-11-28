package receiver_finalizer_property_invalid
public open class Parent { public prop value: Int64 { get() { 7 } } }
public class Child <: Parent { ~init() { let observed = super.value } }
