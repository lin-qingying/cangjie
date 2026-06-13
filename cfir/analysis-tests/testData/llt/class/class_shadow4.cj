// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class Base {
    public let val = 1
}

open class Data <: Base {
    public static let val2 = 2
}

class Derived <: Data {
    <!INHERIT_MEMBER_KIND_INCONSISTENT!>p<!>ublic static let val = 3
}

main(): Int64 {
    var res = Derived.val
    return 0
}
