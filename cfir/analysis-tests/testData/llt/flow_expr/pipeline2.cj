// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func g(b: Bool) {
    !b
}

func run(): Bool {
    let res = true || true |> g
    return res
}

main(): Int64 {
    let res = run()
    if (!res) {
        return 0
    }
    return 1
}

// ASSERT: scan-not core dumped
// ASSERT: scan-not stack trace
