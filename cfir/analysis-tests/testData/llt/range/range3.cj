// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %noColor %f 2>&1 | compare %f

main() {
    var range1 = <!parse_expected_expression!>..<!>10 :3
    for (i in range1) {
        i
    }

    for (i in 1..<!parse_expected_expression!>:<!>3) {
        i
    }
    return 0
}
