// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// RUN-EXEC: %run %run_opt %output %run_args

interface I {}

class A <: I {}

func f(): Any {
    return A()
}

main(): Int64 {
    let i: I = (f() as I).getOrThrow()
    let v = i as A
    if (let Some(x) <- v) {
        return 0
    } else {
        return 1
    }
}
