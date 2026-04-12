// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

main(): Int64 {
    return 0
}

class A {
    public var a0 = C().c
    public var a1 = B.b
}

class B {
    public static var b: Int64 = 1
}

class C {
    public var c: Int64 = 1
}
