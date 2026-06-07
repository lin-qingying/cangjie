// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main() {
    try {
    } catch (<!parse_expected_wildcard_or_exception_pattern!>(<!>a, b)<!parse_unmatched_right_delimiter!>)<!> {
        println(a, b)
    }
<!parse_expected_expr_or_decl_in!>}<!>
