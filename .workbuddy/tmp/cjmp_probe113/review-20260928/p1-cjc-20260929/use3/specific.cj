package cjmp_default_before_implicit

public specific func a(x!: Int64): Int64 { x }
public func use3() { a() }
public func typeWitness(): Int64 { use3() }

