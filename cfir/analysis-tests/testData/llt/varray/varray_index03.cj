// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main() {
    var a = VArray<Int64, $<!LITERAL_NUMERIC_OVERFLOW!>18446744073709551616<!>>(repeat: 0)
    a[9223372036854775807 + 1]
    a[-9223372036854775808 - 1]

    var b = VArray<Int64, $9223372036854775807>(repeat: 0)
    <!BUILTIN_INDEX_IN_BOUND!>b<!>[9223372036854775807] // error

    var c = VArray<Int64, $1>(repeat: 0)
    c[9223372036854775807 + 1]
    c[-9223372036854775808 - 1]
    0
}
