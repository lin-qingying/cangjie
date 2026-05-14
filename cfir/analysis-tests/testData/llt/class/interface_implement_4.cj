// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I1 {
    func foo(): Int64 {
        return 1
    }
}

interface I2 {
    func foo(): Int64 {
        return 2
    }
}

open <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class C1<!> <: I1 & I2 {}

open class C2 <: I1 & I2 {
    public func foo(): Int64 {
        return 3
    }
}

main(): Int64 {
    var res = C1().<!AMBIGUOUS_FUNCTION_CALL!>foo<!>()
    if (res == 1) {
        return 0
    }
    return 1
}
