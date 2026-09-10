public func directTuple(): Int64 {
    match ((1, 2)) {
        case <!UNREACHABLE_PATTERN!>(_: Int32, _)<!> => 0
        case (_: Int64, _) => 1
        case <!UNREACHABLE_PATTERN!>_<!> => 2
    }
}
public func nestedTuple(): Int64 {
    match (((1, 2), 3)) {
        case <!UNREACHABLE_PATTERN!>((_: Int32, _), _)<!> => 0
        case _ => 1
    }
}
public func defaultFloat(): Int64 {
    match ((1.0, 2.0)) {
        case <!UNREACHABLE_PATTERN!>(_: Float32, _)<!> => 0
        case _ => 1
    }
}
public func mixedTuple(): Int64 {
    match ((1i32, 2)) {
        case <!UNREACHABLE_PATTERN!>(_: Int32, _: Int32)<!> => 0
        case (_: Int32, _: Int64) => 1
        case <!UNREACHABLE_PATTERN!>_<!> => 2
    }
}
public func declaredTuple(): Int64 {
    let value: (Int32, Int32) = (1, 2)
    match (value) {
        case (_: Int32, _) => 1
        case <!UNREACHABLE_PATTERN!>_<!> => 2
    }
}
class Box<T> {
    let value: T
    init(value: T) { this.value = value }
    public func get(): T { value }
}
public func genericTuple(): Int64 {
    let value = Box((1, 2))
    match (value.get()) {
        case <!UNREACHABLE_PATTERN!>(_: Int32, _)<!> => 0
        case _ => 1
    }
}
