// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I1<T> {
    func foo(a: T): Int64 {
        1
    }
}

interface I2<T> {
    func foo(a: T): Int64 {
        2
    }
}

class C1<T> {}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>extend<T> C1<T> <: I1<T> {}<!>

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>extend<T> C1<T> <: I2<T> {}<!>

main() {
    return -1
}
