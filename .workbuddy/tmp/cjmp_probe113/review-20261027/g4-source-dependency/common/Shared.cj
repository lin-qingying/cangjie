package g4consumer

private import g4provider.Receiver

public class CommonReceiver {
    public init() {}
}

extend CommonReceiver {
    public func commonValue(): Int64 {
        31
    }
}

// common-part 内的常规 import 也必须能取得依赖包导出的扩展。
public func commonCallsRegular(): Int64 {
    Receiver().visibleValue()
}
