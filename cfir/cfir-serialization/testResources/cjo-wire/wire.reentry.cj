package wire.reentry

@Annotation[target: [Type]]
public class Mark {
    public const init() {}
}

public class Holder {
    public var member: Int64 = 1
    public static var shared: Int64 = 2
    public prop value: Int64 {
        get() { member }
    }
}
