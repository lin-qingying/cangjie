// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix %run_args 2>&1 | compare %f
// ASSERT: regex 2323

internal import std.collection.*

main() {
    let a: Array<Int32> = [1, 2, 3]
    for (i in a where i > 1) {
        print(i.toString())
    }
    let t = ArrayList<Int32>([1, 2, 3])
    for (i in t where i > 1) {
        print(i.toString())
    }
    0
}
