package p1_both_implicit_good

public specific func value() { 2 }
public func use() { value() }
public func typeWitness(): Int64 { use() }

