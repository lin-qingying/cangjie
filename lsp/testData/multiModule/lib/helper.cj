package sample.lib

public func helper(): Int64 {
    return 42
}

public class LibType {
    public func value(): Int64 {
        return helper()
    }
}
