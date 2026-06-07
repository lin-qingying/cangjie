// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    var c =1
    let a = [2,1,0]
    c = a[1 << 1@#<!lex_expected_quote_in_raw_string!>]<!>
    c++
    c--
    return 0
}
