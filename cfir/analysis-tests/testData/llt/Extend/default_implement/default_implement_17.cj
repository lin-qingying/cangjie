// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


interface I<T> {
    func foo(a: T): Unit {
        println(1)
    }
}

interface I1<T> <: I<T> {}

class A<T> {}

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend A<Int64> <: I<Int64> {}

extend<T> A<T> <: I1<T> {}

class B<T> {}

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend B<Int64> <: I1<Int64> {}

extend<T> B<T> <: I<T> {}

main() {
    A<Int64>().foo(1)
}
