// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

main(): Int64 {
    let b = [[1u8, 2u8, 3u8]]
    <!sema_parameters_and_arguments_mismatch!>b<!>[<!sema_cannot_convert_literal!>1<!>.01][0] != 1
    return 0
}
