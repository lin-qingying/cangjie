// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func run(): Int64 {
    var bu: Int8 = -128 - (-1)
    var cu: Int16 = -32768 - (-1)
    var eu: Int32 = -2147483648 - (-1)
    var fu: Int64 = -9223372036854775808 - (-1)
    var bu1: Int16 = 12 - (-12)
    bu1 = 1 - (-1)

    return 0
}

main(): Int64 {
    return run();
}
