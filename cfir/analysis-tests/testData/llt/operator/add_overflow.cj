// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func run(): Int64 {
    var _: Int8 = <!CONST_EVAL_ARITHMETIC_OVERFLOW!>127 + 1<!>
    var _: Int16 = <!CONST_EVAL_ARITHMETIC_OVERFLOW!>32767 + 1<!>
    var _: Int32 = <!CONST_EVAL_ARITHMETIC_OVERFLOW!>2147483647 + 1<!>
    var _: Int64 = <!CONST_EVAL_ARITHMETIC_OVERFLOW!>9223372036854775807 + 1<!>
    var _: UInt8 = <!CONST_EVAL_ARITHMETIC_OVERFLOW!>255 + 1<!>
    var _: UInt16 = <!CONST_EVAL_ARITHMETIC_OVERFLOW!>65535 + 1<!>
    var _: UInt32 = <!CONST_EVAL_ARITHMETIC_OVERFLOW!>4294967295 + 1<!>
    var _: UInt64 = <!CONST_EVAL_ARITHMETIC_OVERFLOW!>18446744073709551615 + 1<!>
    var _: UInt64 = <!CONST_EVAL_ARITHMETIC_OVERFLOW!>(4294968200 << 31) + 9223373978179993602<!>

    return 0
}

main(): Int64 {
    return run();
}
