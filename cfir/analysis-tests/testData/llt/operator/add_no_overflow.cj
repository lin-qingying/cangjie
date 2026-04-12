// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func run(): Int64 {
    var a0: Int8 = 126 + 1
    var a1: Int8 = -127 + -1
    var a2: Int16 = 32766 + 1
    var a3: Int16 = -32767 + -1
    var a4: Int32 = 2147483646 + 1
    var a5: Int32 = -2147483647 + -1
    var a6: Int64 = 9223372036854775806 + 1
    var a7: Int64 = -9223372036854775807 + -1
    var a8: UInt8 = 254 + 1
    var a9: UInt16 = 65534 + 1
    var a10: UInt32 = 4294967294 + 1
    var a11: UInt64 = 18446744073709551614 + 1

    if (a0 != 127) {
        return 1
    }
    if (a1 != -128) {
        return 1
    }
    if (a2 != 32767) {
        return 1
    }
    if (a3 != -32768) {
        return 1
    }
    if (a4 != 2147483647) {
        return 1
    }
    if (a5 != -2147483648) {
        return 1
    }
    if (a6 != 9223372036854775807) {
        return 1
    }
    if (a7 != -9223372036854775808) {
        return 1
    }
    if (a8 != 255) {
        return 1
    }
    if (a9 != 65535) {
        return 1
    }
    if (a10 != 4294967295) {
        return 1
    }
    if (a11 != 18446744073709551615) {
        return 1
    }

    return 0
}

main(): Int64 {
    return run();
}
