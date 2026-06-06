// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE)EXEC-PIPE: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// (CJNATIVE)EXEC: %run %run_opt %output %run_args

func f(x: Int64) {
    match (x) {
        case <!sema_unreachable_pattern!>_: Nothing<!> => -1 // unreachable
        case _: ToString => 0
    }
}

main() {
    <!chir_dce_unreachable!>f<!>(return 0)
}
