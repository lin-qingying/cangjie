// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

open class Base {
    public let val = 1
}

open class Data <: Base {
    public func <!INHERIT_MEMBER_KIND_INCONSISTENT!>val<!>() {
        return 0
    }
}

main(): Int64 {
    return 0
}
