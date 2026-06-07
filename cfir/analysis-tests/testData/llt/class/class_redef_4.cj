// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


open class C1 {
    public func foo(): Int64 {
        return 2
    }
}

class C2 <: C1 {
    public static func <!INHERIT_MEMBER_KIND_INCONSISTENT!>foo<!>(): Int64 {
        return 3
    }
}

main(): Int64 {
    return 1
}
