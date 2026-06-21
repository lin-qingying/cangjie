


var MinTickValue: Int64 = 1

class DateTime {
    private var Ticks: Int64 = MinTickValue
    private func SetTick(tick: Int64) {
        Ticks = tick
    }

    public init() {
        Ticks = MinTickValue
    }

    public static func MaxValue(): DateTime {
        var d = DateTime()
        d.SetTick(MinTickValue)
        <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>S<!>etTick(MinTickValue)
        var a = <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>T<!>icks
        return d
    }
}
