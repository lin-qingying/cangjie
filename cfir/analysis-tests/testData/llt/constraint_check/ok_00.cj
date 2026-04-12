// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

interface I<T> {}

func f1<T1>(x: ?T1): Unit where T1 <: I<T1> {}

func f2<T2>(x: T2): Unit where T2 <: I<T2> {
    f1(x) // ok, x --> Some(x), T1 --> T2
}

main() {}
