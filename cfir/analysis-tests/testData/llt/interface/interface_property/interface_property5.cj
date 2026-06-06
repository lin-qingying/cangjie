// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

open class A {}

class B <: A {}

interface I1 {
    prop a: A {
        get() {
            return A()
        }
    }
}

interface I2 {
    prop a: B {
        get() {
            return B()
        }
    }
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class <!INHERIT_MEMBER_TYPE_INCONSISTENT!>C<!><!> <: I1 & I2 {}

main() {
    return 0
}
