<!MISSING_ENTRY!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// CHECK_PROGRAM_ENTRY
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface Base {
    static func foo(a: Int64) {
        return a
    }
}

abstract class Data <: Base {
    public func <!INHERIT_MEMBER_KIND_INCONSISTENT!>foo<!>() {
        return 1
    }
}
