// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-0: %compiler %cmp_opt %f -o %output
// EXEC-0: %run %run_opt %output %run_args

interface I1 {
    func f1(): Int32
    func f2(): Int32 {
        0
    }
}

interface I2 {
    func f1(): Int32 {
        0
    }
    func f2(): Int32 {
        0
    }
}

class C <: I1 & I2 {
    public override func f2(): Int32 {
        0
    }
}

main(): Int64 {
    return Int64(C().f2())
}
