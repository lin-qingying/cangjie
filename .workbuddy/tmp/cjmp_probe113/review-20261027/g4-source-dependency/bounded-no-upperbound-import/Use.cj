package g4consumer

private import g4provider.{ProbeBox, Fits}

public func useBoundedExtension(): Int64 {
    ProbeBox<Fits>().boundedValue()
}
