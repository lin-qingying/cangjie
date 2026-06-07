// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


func run(): Int64 {
    var _: Int8 = -127 - 1
    var _: Int8 = <!chir_arithmetic_operator_overflow!>-128 - 1<!>
    var _: Int8 = <!chir_arithmetic_operator_overflow!>127 - (-1)<!>
    var _: Int16 = -32767 - 1
    var _: Int16 = <!chir_arithmetic_operator_overflow!>-32768 - 1<!>
    var _: Int16 = <!chir_arithmetic_operator_overflow!>32767 - (-1)<!>
    var _: Int32 = -2147483647 - 1
    var _: Int32 = <!chir_arithmetic_operator_overflow!>-2147483648 - 1<!>
    var _: Int32 = <!chir_arithmetic_operator_overflow!>2147483647 - (-1)<!>
    var _: Int64 = -9223372036854775807 - 1
    var _: Int64 = <!chir_arithmetic_operator_overflow!>-9223372036854775808 - 1<!>
    var _: Int64 = <!chir_arithmetic_operator_overflow!>9223372036854775807 - (-1)<!>
    var _: UInt8 = <!chir_arithmetic_operator_overflow!>0 - 1<!>
    var _: UInt16 = <!chir_arithmetic_operator_overflow!>0 - 1<!>
    var _: UInt32 = <!chir_arithmetic_operator_overflow!>0 - 1<!>
    var _: UInt64 = <!chir_arithmetic_operator_overflow!>0 - 1<!>

    var _: Int8 = <!chir_arithmetic_operator_overflow!>-128 / -1<!>
    var _: Int16 = <!chir_arithmetic_operator_overflow!>-32768 / -1<!>
    var _: Int32 = <!chir_arithmetic_operator_overflow!>-2147483648 / -1<!>
    var _: Int64 = <!chir_arithmetic_operator_overflow!>-9223372036854775808 / -1<!>

    var _: Int8 = <!chir_arithmetic_operator_overflow!>-(-127 - 1)<!>
    var _: Int16 = <!chir_arithmetic_operator_overflow!>-(-32767 - 1)<!>
    var _: Int32 = <!chir_arithmetic_operator_overflow!>-(-2147483647 - 1)<!>
    var _: Int64 = <!chir_arithmetic_operator_overflow!>-(-9223372036854775807 - 1)<!>
    var _: UInt8 = <!chir_arithmetic_operator_overflow!>-(1 + 2)<!>
    var _: UInt16 = <!chir_arithmetic_operator_overflow!>-(1 + 2)<!>
    var _: UInt32 = <!chir_arithmetic_operator_overflow!>-(1 + 2)<!>
    var _: UInt64 = <!chir_arithmetic_operator_overflow!>-(1 + 2)<!>

    return 0
}

main(): Int64 {
    return run();
}
