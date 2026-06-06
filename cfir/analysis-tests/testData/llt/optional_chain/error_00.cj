// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

class C {
<!parse_expected_decl!>_args<!>

<!parse_unexpected_declaration_in_scope!>class<!> C {
    public var item: Int64 = 100
}
<!parse_unexpected_declaration_in_scope!>main<!>(): Int64 {
    var c = C()
    let c1 = Option<C>.Some(c)
    c.item = 50
    let r1 = c1?.item
    let res = r1 ?? 0 <!parse_expected_character!>50<!>
<!parse_expected_expr_or_decl_in!>}<!><!parse_expected_right_delimiter!>
<!>
