// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

class A<T> {}

interface I {
    func i1() {
        44
    }
}

extend<M, N> A<(M, N)> <: I {
    func goo() {
        33
    }
}

main() {
    let a = A<(Rune, Int64, Rune)>()
    let v1 = a.<!NOT_MEMBER_OF!>goo<!>()

    let v2: I = <!TYPE_MISMATCH!>a<!>
    return 0
}
