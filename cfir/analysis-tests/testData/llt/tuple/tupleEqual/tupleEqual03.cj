// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

main(): Int64 {
    let t1 = (((1, 1), 2), (1, 2), (1, (1, (1, (1, (1, 2, 3))))));
    let t2 = (((1, 1), 2), (1, 2), (1, (1, (1, (1, (1, 2, 3))))));
    let t3 = (((1, 1), 2), (1, 2), (1, (1, (1, (1, (1, 3, 3))))));

    if (t1 != t2) {
        return 1;
    }
    if (t1 == t3) {
        return 2;
    }
    return 0;
}
