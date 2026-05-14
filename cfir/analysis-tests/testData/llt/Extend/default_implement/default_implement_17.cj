// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface I<T> {
    func foo(a: T): Unit {
        println(1)
    }
}

interface I1<T> <: I<T> {}

class A<T> {}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>extend A<Int64> <: I<Int64> {}<!>

extend<T> A<T> <: I1<T> {}

class B<T> {}

<!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>extend B<Int64> <: I1<Int64> {}<!>

extend<T> B<T> <: I<T> {}

main() {
    A<Int64>().foo(1)
}
