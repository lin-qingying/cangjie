// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

open class A {
    public var a: Int64
    public init(b: Int64) {
        a = b
    }
    public init() {
        a = 0
    }
}

class X <: A {
    public let c: Int64 = 0
}

main(): Int64 {
    return 0
}
