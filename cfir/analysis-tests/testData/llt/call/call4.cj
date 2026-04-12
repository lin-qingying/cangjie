// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

open class A {}

open class B <: A {}

open class C <: B {}

class D <: C {}

func foo<X>(a: X, b: X, c: (X) -> Unit): Int64 {
    return 1
}

func foo(a: B, b: C, c: (A) -> Unit): Int64 {
    return 2
}

main() {
    var a = foo(B(), D(), {d: A =>})
    if (a != 2) {
        return 1
    }
    return 0
}
