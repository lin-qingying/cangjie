// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

// Was ICE, should quit normally
main() {
    try (<!parse_expected_name!>)<!> <!parse_expected_left_brace!>/<!>he/ no <!parse_expected_character!>a<!> re.
    {
    <!parse_expected_double_arrow_in_lambda!>}<!> <!parse_expected_character!>finally<!> {}
<!parse_expected_expr_or_decl_in!>}<!>

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
