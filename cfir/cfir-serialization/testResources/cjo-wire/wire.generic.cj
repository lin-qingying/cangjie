package wire.generic

public interface Link<T> where T <: Link<T> {}

public class Box<T> where T <: Link<T> {
    public let value: T
    public init(value: T) { this.value = value }
}

public func identity<T>(value: T): T { value }
