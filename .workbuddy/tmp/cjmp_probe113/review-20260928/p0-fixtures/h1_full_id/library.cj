package p0_h1_full_id

public class Envelope<T> {
    public init() {}
    public func echo(value: T): T { value }
}

public func retainString(value: String): String { value }

public func retainObject(value: Object): Object { value }
