// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

enum E<T0> {
    | S(T0)
    | N
}

interface I<T1> {
    func foo(): Unit
}

extend<T2> E<T2> <: I<E<T2>> {
    public func foo(): Unit {
        let x: E<E<T2>> = E<E<T2>>.N
    }
}

main() {}
