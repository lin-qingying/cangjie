// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %output
// EXEC: %run %run_opt %output %run_args

interface I {}

interface I1<T> {}

enum E <: I {
    EE
}

enum E1<T> <: I1<Int32> {
    EE1
}

enum E2<T> <: I1<T> {
    EE2
}

main(): Int64 {
    let a: I = EE
    let b: I1<Int32> = EE1<Int32>
    let c: I1<Rune> = EE2
    if (a is E && b is E1<Int32> && c is E2<Rune>) {
        return 0
    }
    return 1
}
