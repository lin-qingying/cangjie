// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

public struct Record45 {
    public init() {}

    public static var var131: Int32 = 0;
    public var lambda = {=> var131}
    public static var lambda1 = {=> var131}
    public var v = var131
    public init(a: Int8) {
        this()
        var lambda1 = {
            =>
            var131 = var131 + 1
            return var131
        }
        lambda1()
    }
}

public struct Record46 {
    public init() {}

    public static var var131: Int32 = 0;
    public var lambda = {=> var131}
    public static var lambda1 = {=> var131}
    public var v = var131
    public init(a: Int8) {
        this()
        var lambda1 = {
            =>
            var131 = var131 + 1
            return var131
        }
        lambda1()
    }
}

main(): Int64 {
    var rec = Record45(2)
    if (rec.lambda() != 1 && Record45.lambda1() != 1) {
        return 1
    }
    if (Record45.lambda1() != 1 && Record45(2).lambda() != 2) {
        return 1
    }
    return 0
}
