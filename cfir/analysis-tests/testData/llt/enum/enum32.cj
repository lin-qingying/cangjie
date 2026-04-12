// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// EXEC: %run %run_opt %output %run_args

interface I<T> {
    func f(): I<T>
}

enum E <: I<E> {
    E1 | E2

    public func f(): I<E> {
        return this
    }
}

main(): Int64 {
    0
}
