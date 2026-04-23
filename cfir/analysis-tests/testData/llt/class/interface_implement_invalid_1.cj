// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

interface I1 {
    func foo(): Int64
}

interface I2 <: I1 {}

interface C1 <: I2 {}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C2<!> <: I2 {}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C3<!> <: I2 {
    public func foo(a: Int64): Int64 {
        return 0
    }
}

class C4 <: I2 {
    public func foo(): Int64 {
        return 0
    }
}

main(): Int64 {
    return 0
}
