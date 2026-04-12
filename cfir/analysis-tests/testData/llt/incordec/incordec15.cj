// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

struct R {
    public let r: Int32
    public init(x: Int32) {
        r = x
    }
    public operator func +(inputB: R): R {
        return R(this.r + inputB.r)
    }
    public operator func -(inputB: R): Bool {
        return true
    }
}

main(): Int64 {
    var r: R = R(-1)
    var r1: R = r + R(1)
    if (r - R(1)) {
        return 0
    }
    return 1
}
