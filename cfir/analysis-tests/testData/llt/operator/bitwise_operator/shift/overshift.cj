// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    var _: Int8 = <!chir_shift_length_overflow!>10 << 8<!>
    var _: Int8 = <!chir_shift_length_overflow!>10 << 9<!>
    var _: Int8 = <!chir_shift_length_overflow!>10 >> 9<!>
    var _: Int16 = <!chir_shift_length_overflow!>10 << 16<!>
    var _: Int16 = <!chir_shift_length_overflow!>10 << 17<!>
    var _: Int32 = <!chir_shift_length_overflow!>10 >> 33<!>
    var _: Int64 = <!chir_shift_length_overflow!>10 >> 65<!>
    var _: UInt8 = <!chir_shift_length_overflow!>10 >> -1<!>
    var _: UInt8 = <!chir_shift_length_overflow!>10 >> 10<!>
    var _: UInt16 = <!chir_shift_length_overflow!>10 >> 18<!>
    var _: UInt32 = <!chir_shift_length_overflow!>10 >> 33<!>
    var _: UInt64 = <!chir_shift_length_overflow!>10 << 128<!>
    var rfa: UInt8 = 10
    <!chir_shift_length_overflow!>rfa << 9<!>
    <!chir_shift_length_overflow!>127 << -1<!>
    var _ = <!chir_shift_length_overflow!>10 << 65<!>
    return 0;
}
