// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler  %cmp_opt %f -o %output 2>&1 | compare %f
// ASSERT: scan : undeclared identifier 'G'
// ASSERT: regex-not type of function body is incompatible with return type
// ASSERT: scan-not core dumped
// ASSERT: scan-not stack trace

internal import std.collection.*

func f(input: ArrayList<Int64>): Int64 {
    0
}

main(): Int64 {
    return ArrayList<Int64>([1]) |> f
    G
}
