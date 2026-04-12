// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

main(): Int64 {
    if (Data.DAY.n != 1) {
        return 1
    }
    if (Data.DAY.m != 2) {
        return 2
    }
    return 0
}

enum Data {
    DAY
}

extend Data {
    public prop m: Int64 {
        get() {
            return 2
        }
    }
    public prop n: Int64 {
        get() {
            return 1
        }
    }
}
