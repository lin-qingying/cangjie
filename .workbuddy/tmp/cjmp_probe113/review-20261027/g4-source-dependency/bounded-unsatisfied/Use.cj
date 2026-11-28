package g4consumer

private import g4provider.{ProbeBox, Bound, Misses}

public func useBoundedExtension(): Int64 {
    ProbeBox<Misses>().boundedValue()
}
