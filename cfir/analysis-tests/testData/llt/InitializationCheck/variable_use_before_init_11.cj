// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

struct A {
    var x = 0
}

struct B {
    var a: A
    init() {
        <!USED_BEFORE_INITIALIZATION!>a<!>.x = 1
        a = A()
    }
}

main() {
    println(B().a.x)
}
