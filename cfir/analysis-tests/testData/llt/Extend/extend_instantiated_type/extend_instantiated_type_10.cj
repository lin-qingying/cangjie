// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

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
    let a = A<(Rune, Int64)>()
    let v1 = a.goo()
    if (v1 != 33) {
        return 1
    }
    let v2: I = a
    let v3 = v2.i1();
    if (v3 != 44) {
        return 2
    }
    return 0
}
