// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC-36: %run %run_opt %output %run_args

func composition(f: (Int64) -> Int64, g: (Int64) -> Int64): (Int64) -> Int64 {
    return {x: Int64 => f(g(x))}
}

func f(x: Int64): Int64 {
    x + x
}

func g(x: Int64): Int64 {
    x * x
}

main() {
    let fg = f ~> g
    fg(3)
}
