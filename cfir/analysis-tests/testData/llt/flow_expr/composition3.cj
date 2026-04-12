// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func f(x: Int32): Float32 {
    Float32(x) + 1.0
}

func g(x: Float32): Int32 {
    Int32(x * 2.0)
}

func run() {
    let x: Int32 = 1
    let fg3 = f ~> g ~> f ~> g ~> f ~> g
    let res = fg3(x)
    print("res: ${res}")
    return res
}

main(): Int64 {
    let res = run()
    if (res == 22) {
        return 0
    }
    return 1
}

// ASSERT: scan-not core dumped
// ASSERT: scan-not stack trace
