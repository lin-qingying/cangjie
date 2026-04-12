// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

func run(): Int64 {
    var au = -9223372036854775808
    var b: Int8 = 127
    var bu: Int8 = -128
    var ub: UInt8 = 255
    var ubu: UInt8 = 0

    var c: Int16 = 32767
    var cu: Int16 = -32768
    var uc: UInt16 = 65535
    var ucu: UInt16 = 0

    var e: Int32 = 2147483647
    var eu: Int32 = -2147483648
    var ue: UInt32 = 4294967295
    var ueu: UInt32 = 0

    var f: Int64 = 9223372036854775807
    var fu: Int64 = -9223372036854775808
    var uf: UInt64 = 18446744073709551615
    var ufu: UInt64 = 0
    return 0
}

main(): Int64 {
    return run();
}
