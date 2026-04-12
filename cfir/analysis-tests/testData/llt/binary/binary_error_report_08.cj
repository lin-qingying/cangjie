// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output

main(): Int64 {
    const x: Int8 = -20;
    1 + (x / (7 | 1) + (if (true) {
        x
    } else {
        0
    } + 0));
    return 0;
}
