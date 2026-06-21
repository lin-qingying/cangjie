

struct Date {
    private let Date: Int64 = 5
    private let Date1: Int64 = 6

    public func get_pub_Date(): Int64 {
        return Date
    }
    public let pub_modifier: () -> Int64 = <!USED_BEFORE_INITIALIZATION!>get_pub_Date<!>
    private let pri_modifier: () -> Int64 = <!USED_BEFORE_INITIALIZATION!>get_pub_Date<!>

    private func get_pri_Date1(): Int64 {
        return Date1
    }
    public let pub_modifier1: () -> Int64 = <!USED_BEFORE_INITIALIZATION!>get_pri_Date1<!>
    private let pri_modifier1: () -> Int64 = <!USED_BEFORE_INITIALIZATION!>get_pri_Date1<!>
}

main(): Int64 {
    var newRec1: Date = Date()
    if (newRec1.pub_modifier() != 5 || newRec1.pub_modifier1() != 6) {
        return 1
    }
    return 0
}
