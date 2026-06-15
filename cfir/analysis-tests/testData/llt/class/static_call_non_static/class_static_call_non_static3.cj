<!MISSING_ENTRY!>/<!>/ Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// CHECK_PROGRAM_ENTRY
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class Base {
    public func f(): Int32 {
        return 1
    }
    public static func get(): Int32 {
        var a = <!STATIC_FUNCTION_CANNOT_ACCESS_NON_STATIC_MEMBER!>f<!>
        return a()
    }
}
