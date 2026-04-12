// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

struct a {
    public let b = 105
    public let c = d()
    public init() {
        match {
            case b >= b =>
                var e = d();
                e
            case _ => c
        }.f()
    }
}

class d {
    public var g = (1, h())

    public func f(i!: h = g[1]) {}
}

class h {}

main(): Int64 {
    return 0;
}
