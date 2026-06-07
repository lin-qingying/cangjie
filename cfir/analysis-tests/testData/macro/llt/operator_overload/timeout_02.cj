// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


import define.*

main() {
    var a = @SAME<!parse_expected_decl!>{<!>1}() <= @SAME<!parse_expected_decl!>{<!>1}()
    var b = @SAME[0]<!parse_expected_decl!>{<!>1}() <= @SAME[0]<!parse_expected_decl!>{<!>1}()
    if ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == ((a == b) && (a == true))<!parse_expected_right_delimiter!>
<!>    {
        return 0
    }<!parse_expected_right_delimiter!>
<!>    else<!parse_expected_right_delimiter!>
<!>    {
        return 1
    }<!parse_expected_right_delimiter!>
<!>}<!parse_expected_right_delimiter!>
<!>
