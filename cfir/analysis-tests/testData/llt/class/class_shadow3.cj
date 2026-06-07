// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class Base {
    public let val: Int32 = 0
}

class Data <: Base {
    <!sema_member_variable_can_not_shadow!>p<!>ublic let val: Int32 = 1
}

open class DataI {
    public static let val: Int32 = 4
}

class DataIDerived <: DataI {
    <!sema_static_and_non_static_member_cannot_have_same_name!>p<!>ublic let val: Int32 = 5
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
