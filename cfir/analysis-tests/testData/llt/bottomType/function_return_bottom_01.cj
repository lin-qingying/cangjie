// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE) EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// (CJNATIVE) EXEC: %run %run_opt %output %run_args

main(): Int64 {
    case01()
    return 0
}

func case01() {
    <!chir_dce_unreachable_expression_hint!>while (return ()) {
        print("yes")
    }<!>
}
