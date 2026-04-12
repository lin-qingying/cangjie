// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func f(x: Float32): Float32 {
    x + 1.0
}

func run() {
    let x: Float32 = 1.0
    let y: Float32 = 11.0
    let z: Float32 = 22.0
    let res = if (x > 0.0) {
        y
    } else {
        z |> f
    }
    return res
}

main(): Int64 {
    let res = run()
    if (res == 11.000000) {
        return 0
    }
    return 1
}

// ASSERT: scan-not core dumped
// ASSERT: scan-not stack trace
