// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

class C {
    static var i = 1
}

class A<T> where T <: C {}

extend<R> A<R> {
    func f() {
        return R.i
    }
}

main() {
    return A<C>().f() - 1
}
