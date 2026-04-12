// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// RUN-EXEC: %run %run_opt %output %run_args

interface I1<T> {
    func f(): Array<T> {
        []
    }
    func g() {
        f()
    }
}

class A<N> {}

extend A<Int64> <: I1<Int64> {
    // func foo(a: Int64) {}
}

extend A<String> <: I1<String> {
    // func foo(a: Int64) {}
}

main() {
    return 0
}
