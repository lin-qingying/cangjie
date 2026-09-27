package cjmp_p

public common class S {
    public common init() {}
    public common static let x: Int64
}

public class U {
    public init() {}
    static init() {
        S.x = 1
    }
}
