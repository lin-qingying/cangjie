package receiver_finalizer_field
public open class Parent { public var value: Int64 = 7 }
public class Child <: Parent {
    @Frozen
    ~init() { let observed = super.value }
}
