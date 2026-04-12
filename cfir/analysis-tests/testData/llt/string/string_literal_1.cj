// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix  | compare %f
// ASSERT: scan ghi

internal import std.collection.*

main() {
    var x: Collection<Rune> = "abc".toRuneArray()

    var y = "def"
    let st1 = HashSet<Rune>(y.toRuneArray())

    let st2 = HashSet<Rune>("ghi".toRuneArray())

    for (i in st2) {
        print(i)
    }
}
