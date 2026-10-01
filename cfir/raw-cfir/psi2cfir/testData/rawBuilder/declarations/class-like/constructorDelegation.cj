// RUN_PIPELINE_TILL: FRONTEND
open class DelegBase {
    public var v: Int64 = 0

    public init(x: Int64) {
        this.v = x
    }
}

class DelegDerived <: DelegBase {
    var w: Int64 = 0

    init() {
        super(1)
        this.w = 2
    }

    init(y: Int64) {
        this()
        this.w = y
    }
}
