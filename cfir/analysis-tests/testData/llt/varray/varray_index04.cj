// (CJNATIVE and DEBUG_OPTION) EXEC-PIPE: %frontendCompiler %dump-ast %cmp_opt %f -o %output 2>&1 | compare %f

main() {
    // UInt32_Max = 4294967295
    var a = VArray<Int64, $4294967297>(repeat: 0)
    <!chir_idx_out_of_bounds!>a[4294967296]<!> // pass
}

// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
