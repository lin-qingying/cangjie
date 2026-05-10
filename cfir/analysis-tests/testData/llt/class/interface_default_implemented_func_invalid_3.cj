// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

interface I1 {
    func foo1(): Int64 {
        return 0
    }
}

struct R1 <: I1 {}

extend R1 {
    public func <!EXTEND_MEMBER_CANNOT_SHADOW!>foo1<!>(): Int64 {
        return 0
    }
}

main() {
    return -1
}
