

internal import std.collection.*

struct Date {
    public let width: Int32
    public let length: Int32 = <!ILLEGAL_THIS_OUTSIDE_STRUCT_CONSTRUCTOR!>this<!>.width
    public static var height: Int32 = 2
    public init() {
        this.width = 1
        this.<!CANNOT_ASSIGN_TO_IMMUTABLE!>length<!> = 2
        this.f()
        this.f
        height = 3
    }
    public <!RECURSIVE_CONSTRUCTOR_CALL!>init<!>(a: Int64) {
        this
        <!ILLEGAL_PLACE_OF_CALLING_THIS_OR_SUPER!>this<!>(1)
    }
    public static func ff(): Date {
        return <!STATIC_MEMBERS_CANNOT_CALL_MEMBERS!>this<!>
    }
    public func f(): Date {
        return this
    }
    public func g(l: ArrayList<Date>) {
        l.<!NOT_MEMBER_OF!>_add<!>(this)
    }
}
