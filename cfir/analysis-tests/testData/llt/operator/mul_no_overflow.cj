// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func run(): Int64 {
    var m0: Int8 = 18 * 7
    var m1: Int8 = -2 * 64

    var m2: Int16 = 1638 * 20
    var m3: Int16 = 1024 * -32

    var m4: Int32 = 1024 * 1024 * 2047
    var m5: Int32 = -1024 * -1024 * -2048

    var m6: Int64 = 3074457345618258602 * 3
    var m7: Int64 = -8 * -1073741824 * -1073741824

    if (m1 != -128) {
        return 1
    }
    if (m3 != -32768) {
        return 1
    }
    if (m5 != -2147483648) {
        return 1
    }
    if (m7 != -9223372036854775808) {
        return 1
    }
    return 0
}

main(): Int64 {
    return run();
}
