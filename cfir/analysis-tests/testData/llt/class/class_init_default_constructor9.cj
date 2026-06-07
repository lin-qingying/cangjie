// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


class A {
    public init(){ (input<!parse_expected_operator_or_end!>:<!>Int64):Int64=>{1} }<!parse_expected_right_delimiter!>
<!>}

<!parse_unexpected_declaration_in_scope!>main<!>():Int64 {
    A()
    return 0
}<!parse_expected_right_delimiter!>
<!>
