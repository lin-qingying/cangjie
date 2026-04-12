// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args | compare %f
// ASSERT: scan 18
// ASSERT: scan 3.000000
// ASSERT: scan 3.000000

internal import std.math.*

main() {
    println(abs(-18))
    println(trunc(3.18))
    println(floor(3.18))
}
