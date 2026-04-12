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

    public static let var131: Int32 = 0;

    public init(a: Int8) {
        this()
        func function102(): Int32 {
            var131;
        }
    }
}

main(): Int64 {
    Record45()
    return 0;
}
