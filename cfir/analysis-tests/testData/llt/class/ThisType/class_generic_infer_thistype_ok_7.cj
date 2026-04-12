// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

interface I<T> {}

class A<T> <: I<T> {}

class B {
    func test<T>(a: Option<I<T>>): This {
        this
    }
}

main() {
    let ins = B()
    let a = A<Int64>()
    ins.test<Int64>(a)
    0
}
