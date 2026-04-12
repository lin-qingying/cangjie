// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

class Child {
    public var c: Int64
    private var a: Int64 = 4
    public init(a: Int64) {
        this(2, 3)
        var d: Int64 = f2()
        c = 3
    }
    public init(a: Int64, b: Int64) {
        c = 8
    }

    public func f2() {
        return 3;
    }
}

main(): Int64 {
    return 0
}
