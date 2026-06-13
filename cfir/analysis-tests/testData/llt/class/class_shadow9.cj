// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class Base {
    public let val: Int32 = 0
}

class Data <: Base {
    <!MEMBER_VARIABLE_CAN_NOT_SHADOW!>p<!>ublic let val: Int32 = 1
    public let f: Int32 = <!ILLEGAL_THIS_OR_SUPER_CALL!>super<!>.val
}

main(): Int64 {
    var data = Data()
    var d = data.val
    var f = data.f
    if (d != 1 || f != 0) {
        return 1
    }
    return 0
}
