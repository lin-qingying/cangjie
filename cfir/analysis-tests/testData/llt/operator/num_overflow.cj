// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

func run(): Int64 {
    var ao = <!LITERAL_NUMERIC_OVERFLOW!>18446744073709551616<!>
    var auo = <!LITERAL_NUMERIC_OVERFLOW!>-9223372036854775809<!>
    var bo: Int8 = <!LITERAL_NUMERIC_OVERFLOW!>128<!>
    var buo: Int8 = <!LITERAL_NUMERIC_OVERFLOW!>-129<!>
    var ubo: UInt8 = <!LITERAL_NUMERIC_OVERFLOW!>256<!>
    var ubuo: UInt8 = <!LITERAL_NUMERIC_OVERFLOW!>-1<!>

    var co: Int16 = <!LITERAL_NUMERIC_OVERFLOW!>32768<!>
    var cuo: Int16 = <!LITERAL_NUMERIC_OVERFLOW!>-32769<!>
    var uco: UInt16 = <!LITERAL_NUMERIC_OVERFLOW!>65536<!>
    var ucuo: UInt16 = <!LITERAL_NUMERIC_OVERFLOW!>-1<!>

    var eo: Int32 = <!LITERAL_NUMERIC_OVERFLOW!>2147483648<!>
    var euo: Int32 = <!LITERAL_NUMERIC_OVERFLOW!>-2147483649<!>
    var ueo: UInt32 = <!LITERAL_NUMERIC_OVERFLOW!>42949672956<!>
    var ueuo: UInt32 = <!LITERAL_NUMERIC_OVERFLOW!>-1<!>

    var fo: Int64 = <!LITERAL_NUMERIC_OVERFLOW!>9223372036854775808<!>
    var fuo: Int64 = <!LITERAL_NUMERIC_OVERFLOW!>-9223372036854775809<!>
    var ufo: UInt64 = <!LITERAL_NUMERIC_OVERFLOW!>18446744073709551616<!>
    var ufuo: UInt64 = <!LITERAL_NUMERIC_OVERFLOW!>-1<!>
    return 0
}

main(): Int64 {
    return run();
}
