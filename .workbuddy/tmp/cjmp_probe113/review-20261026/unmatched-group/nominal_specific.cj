package cjmp_unmatched_group_probe

public specific class NominalBox<U> {
    public specific init() { }
    public specific func f(value: String): Unit { }
}

public func instantiateString(value: NominalBox<String>): Unit { }
public func instantiateInt64(value: NominalBox<Int64>): Unit { }
