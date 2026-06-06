// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE) EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// (CJNATIVE) EXEC: %run %run_opt %output %run_args

func function20() {
    while (false) <!chir_dce_unreachable_block_in_expression!>{
        1 <!chir_dce_unreachable!>==<!> continue
    }<!>;

    true;
}

main(): Int64 {
    function20()
    return 0;
}
