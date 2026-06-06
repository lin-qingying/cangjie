// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

main(): Int64 {
    var a: Bool = false
    var <!chir_dce_unused_variable!>var195<!>: (Bool, Int64, UInt8, Range<Int64>) = (true, 60, UInt8(175), 179..149 : 8);
    if (a) <!chir_dce_unreachable_block_in_expression!>{
        <!chir_dce_unused_expression!>var195<!>
    }<!> else {
        <!chir_dce_unused_expression!>(true, 60, UInt8(175), 63..=12 : 1)<!>
    }
    return 0
}
