package cjmp_shadow_before_implicit

public specific func platform(): Int64 { 2 }
public func use4() { platform() }
public func typeWitness(): Int64 { use4() }

