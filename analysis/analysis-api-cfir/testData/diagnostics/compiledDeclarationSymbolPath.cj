package probe

public class Holder {
    public let value: Int64

    public init(value: Int64) {
        this.value = value
    }

    public func doubled(): Int64 {
        return value * 2
    }
}

public func makeHolder(): Holder {
    return Holder(1)
}
