package g4provider

public class Receiver {
    public init() {}
}

extend Receiver {
    private func privateValue(): Int64 {
        17
    }

    public func visibleValue(): Int64 {
        privateValue()
    }
}

public interface Bound {}

public class Fits <: Bound {
    public init() {}
}

public class Misses {
    public init() {}
}

public class ProbeBox<T> {
    public init() {}
}

extend<T> ProbeBox<T> where T <: Bound {
    public func boundedValue(): Int64 {
        29
    }
}

// 被扩展类型来自 std.core，本包的直接扩展仅供本包使用。
extend Int64 {
    public func foreignValue(): Int64 {
        this
    }
}

public func localForeignValue(): Int64 {
    let value: Int64 = 7
    value.foreignValue()
}
