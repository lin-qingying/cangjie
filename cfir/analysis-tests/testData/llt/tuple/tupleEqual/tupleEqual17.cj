// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

func foo(): (Int64, Int64) {
    return (1, 2);
}

main(): Int64 {
    let t1 = (1, 2);
    if (t1 != foo()) {
        return 1;
    }
    return 0;
}
