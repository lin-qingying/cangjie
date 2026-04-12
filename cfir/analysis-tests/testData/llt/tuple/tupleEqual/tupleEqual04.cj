// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

main(): Int64 {
    let t1: (Int8, Int16, Int32, Int64, UInt8, UInt16, UInt32, UInt64) = (1, 1, 1, 0, 1, 0, 0, 1);

    if (t1 == (1, 1, 1, 0, 1, 0, 0, 1)) {
        return 0;
    }

    return 1;
}
