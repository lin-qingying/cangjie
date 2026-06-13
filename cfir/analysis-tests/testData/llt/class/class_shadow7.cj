// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class Base {
    public let f = 0
}

class Data <: Base {
    <!INHERIT_MEMBER_KIND_INCONSISTENT!>p<!>ublic static func f(): Int32 {
        return 0
    }
}

main(): Int64 {
    var res = Data.f()
    return 0
}
