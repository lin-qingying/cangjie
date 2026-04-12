// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC-1: %run %run_opt %output %run_args

class C {
    public var r: Int32 = 0
    public init(x: Int32) {
        r = x
    }
    public operator func ==(b: C): Bool {
        return this.r == b.r
    }
}

func test(): Int64 {
    let a = C(0)
    let b = C(1)
    var c = C(1)
    var res = match (c) {
        case _ => -1
    }
    if (res == 0) {
        return 0
    }
    return 1
}

main(): Int64 {
    return test()
}
