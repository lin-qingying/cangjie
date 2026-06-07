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
    public let f: Int32 = <!sema_this_or_super_not_allowed_to_initialize_non_static_member!>s<!>uper.val
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
