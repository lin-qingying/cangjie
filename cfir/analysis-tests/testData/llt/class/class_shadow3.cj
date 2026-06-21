


open class Base {
    public let val: Int32 = 0
}

class Data <: Base {
    <!MEMBER_VARIABLE_CAN_NOT_SHADOW!>p<!>ublic let val: Int32 = 1
}

open class DataI {
    public static let val: Int32 = 4
}

class DataIDerived <: DataI {
    <!INHERIT_MEMBER_KIND_INCONSISTENT!>p<!>ublic let val: Int32 = 5
}

main(): Int64 {
    var objBase = Base()
    var objData = Data()
    var res0: Int32 = objBase.val
    var res1: Int32 = objData.val
    var res4: Int32 = DataI.val
    var objDataIDerived = DataIDerived()
    var res5: Int32 = objDataIDerived.val
    if (res0 != 0 || res1 != 1 || res4 != 4 || res5 != 5) {
        return 1
    }
    return 0
}
