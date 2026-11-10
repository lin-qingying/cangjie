package g4consumer

public import g4provider.Receiver

public func useExtension(): Int64 {
    Receiver().visibleValue()
}
