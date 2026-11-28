package receiver_finalizer_function_invalid
public open class Parent { public func use(): Unit {} }
public class Child <: Parent { ~init() { super.use() } }
