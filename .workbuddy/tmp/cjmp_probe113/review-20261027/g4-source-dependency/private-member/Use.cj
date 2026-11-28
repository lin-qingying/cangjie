package g4consumer

private import g4provider.Receiver

public func usePrivateMember(): Int64 {
    Receiver().privateValue()
}
