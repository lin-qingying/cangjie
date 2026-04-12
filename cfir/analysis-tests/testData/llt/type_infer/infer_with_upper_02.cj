// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

func f1<T>(a: T, b: T): T {
    return a
}

func f2<T>(a: T, b: T): T where T <: Comparable<T> {
    return a
}

main(): Int64 {
    let a = f1(1, 10)
    let b = f2(1, 10)
    let c = f2(-1, -10)
    // Check the types are inferenced to Int64
    var d: Int64 = a
    d = b
    d = c
    let f0 = f1(1.0, 2.0)
    let f1 = f2(1.0, 2.0)
    let f2 = f2(-1.0, -2.0)
    // Check the types are inferenced to Float64
    var ff: Float64 = f0
    ff = f1
    ff = f2
    return 0
}
