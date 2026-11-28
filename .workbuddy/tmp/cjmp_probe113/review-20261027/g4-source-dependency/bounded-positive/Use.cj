package g4consumer

private import g4provider.{ProbeBox, Bound, Fits}

public func useBoundedExtension(): Int64 {
    ProbeBox<Fits>().boundedValue()
}
