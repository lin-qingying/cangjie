// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

main(): Int64 {
    var numOther = <!LITERAL_NUMERIC_OVERFLOW!>18446744073709551615<!>
    println(numOther is Int64)
    println(numOther)
    return 0
}
