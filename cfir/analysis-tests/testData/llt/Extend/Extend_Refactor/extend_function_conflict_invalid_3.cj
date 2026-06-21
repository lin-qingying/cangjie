// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


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

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend<T> C1<T> <: I1<T> {}

<!INTERFACE_MEMBER_MUST_BE_IMPLEMENTED!>e<!>xtend<T> C1<T> <: I2<T> {}

main() {
    return -1
}
