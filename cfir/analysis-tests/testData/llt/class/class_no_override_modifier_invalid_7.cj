// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %n.cj -o %output 2>&1 | compare %f

interface A {
    func foo(a: Int32): Int32 {
        return a
    }
}

interface B<T> {
    func foo(a: T): T {
        return a
    }
}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>class D<!> <: A & B<Int32> {}

main() {
    return 1
}
