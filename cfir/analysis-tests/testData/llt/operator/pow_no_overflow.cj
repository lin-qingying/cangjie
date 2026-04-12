// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func run(): Int64 {
    var p6: Int64 = 2 ** 62
    p6 = 2 ** 62 - 1 + 2 ** 62
    var p7: Int64 = -2 ** 63

    if (p6 != 9223372036854775807) {
        return 1
    }

    return 0
}

main(): Int64 {
    return run();
}
