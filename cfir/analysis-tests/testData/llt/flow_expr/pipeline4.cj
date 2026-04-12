// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

extend Unit {
    public operator func ()(y: Unit): Unit {}
}

var a = 1

func run() {
    (a = 0) |> (a = 0)
    return 0
}

main(): Int64 {
    return run()
}

// ASSERT: scan-not core dumped
// ASSERT: scan-not stack trace
