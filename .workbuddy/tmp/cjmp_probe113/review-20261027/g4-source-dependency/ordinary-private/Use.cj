package g4consumer

private import g4provider.Receiver

public func useExtension(): Int64 {
    Receiver().visibleValue()
}
