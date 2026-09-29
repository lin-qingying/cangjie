package p0_c4_callable_reference

public specific func platform(): Int64 { 2 }

public func apply(operation: () -> Int64): Int64 { operation() }

public func use(): Int64 { apply(platform) }
