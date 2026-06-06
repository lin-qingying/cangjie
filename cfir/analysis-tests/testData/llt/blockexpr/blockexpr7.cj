// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %noColor %cmp_opt_chir2hlir %f -o %n.%suffix 2>&1 | compare %f

func foo(): CString {
    return  {<!parse_expected_double_arrow_in_lambda!>}<!>
}

main() {
    var c: CString =  {<!parse_expected_double_arrow_in_lambda!>}<!>
}
