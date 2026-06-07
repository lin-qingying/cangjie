// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main() : Int64 {
    var a = 1**2
    var b = 1<!lex_unknown_start_of_token!>+<!>&1
    var c = 1<!lex_unknown_start_of_token!>-<!>&1
    var d = 1<!lex_unknown_start_of_token!>*<!>&1
    var e = 1/1
    var f = 1<!lex_unknown_start_of_token!>*<!>*&1
}
