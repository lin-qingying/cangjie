package receiver_finalizer_nested_field
public open class Parent { public var value: Int64 = 7 }
public class Child <: Parent {
    ~init() {
        func readParent(): Int64 { super.value }
        let observed = readParent()
    }
}
