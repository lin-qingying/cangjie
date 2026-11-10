package g4consumer

private import g4provider.Receiver

public func useBothDependencies(): Int64 {
    Receiver().visibleValue() + CommonReceiver().commonValue() + commonCallsRegular()
}
