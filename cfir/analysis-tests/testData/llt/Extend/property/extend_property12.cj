// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f  -o %output
// EXEC: %run %run_opt %output %run_args

main(): Int64 {
    if (Int8.ns != 3) {
        return 3
    }
    if (Int8.ms != 4) {
        return 4
    }
    return 0
}

extend Int8 {
    public static prop ms: Int64 {
        get() {
            return 4
        }
    }
    public static prop ns: Int64 {
        get() {
            return 3
        }
    }
}
