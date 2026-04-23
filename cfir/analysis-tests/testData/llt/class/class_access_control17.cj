// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface Data2 {}

interface Data3 <: Data2 {}

open class Test1 <: Data2 {
    public static func foo(): Int64 {
        return 3
    }
    private static func foo(a: Int64): Int64 {
        return 3
    }
    public func f(): Int64 {
        Test1.<!AMBIGUOUS_USE!>foo<!>
        return 0
    }
}

class Test <: Test1 & Data2 & Data3 {}

main(): Int64 {
    Test1.<!AMBIGUOUS_USE!>foo<!>
    return 0
}
