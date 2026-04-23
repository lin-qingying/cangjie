// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    var _ = 9223372036854775808 % <!LITERAL_NUMERIC_OVERFLOW!>9223372036854775808<!>
    var _ = 9223372036854775808 / <!LITERAL_NUMERIC_OVERFLOW!>9223372036854775808<!>
    // Test for right value which is overflowed to 0
    let _ = <!LITERAL_NUMERIC_OVERFLOW!>-9223372036854775808<!> % <!LITERAL_NUMERIC_OVERFLOW!>-9223372036854775809<!>
    let _ = <!LITERAL_NUMERIC_OVERFLOW!>-9223372036854775808<!> % <!LITERAL_NUMERIC_OVERFLOW!>9223372036854775808<!>
    let _ = <!LITERAL_NUMERIC_OVERFLOW!>-9223372036854775808<!> / <!LITERAL_NUMERIC_OVERFLOW!>-9223372036854775809<!>
    let _ = <!LITERAL_NUMERIC_OVERFLOW!>-9223372036854775808<!> / <!LITERAL_NUMERIC_OVERFLOW!>9223372036854775808<!>
    return 0
}
