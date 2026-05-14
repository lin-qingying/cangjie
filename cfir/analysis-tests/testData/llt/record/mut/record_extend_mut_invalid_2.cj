// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

struct R1 {
    public var a: Int64 = 0
}

extend R1 {
    public func foo1() {
        this.<!CANNOT_MODIFY_VAR!>a<!> = 1
    }

    public func foo2() {
        <!CANNOT_MODIFY_VAR!>a<!> = 1
    }

    public func foo3() {
        this.<!CANNOT_MODIFY_VAR!>a<!> = this.a + 1
    }
}

main() {
    return 1
}
