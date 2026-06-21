public interface NoneTrait {
    func IsNone(): Bool
}

extend<T> Option<T> <: NoneTrait {
    public func IsNone(): Bool
    {
        return match (this) {
            case Some(v) => false
            case None => true
        }
    }
}

main() {
    let b = None<Int64>
    if (!b.IsNone()) {
        return 1
    }
    return 0
}




