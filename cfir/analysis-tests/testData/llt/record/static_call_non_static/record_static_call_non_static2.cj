// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dump
// ASSERT: regex-not stack trace

let var14: Record7 = Record7();

struct Record7 {
    private func function36(): Int32 {
        (-29);
    }

    public func function26(): Int32 {
        var14.function36();
    }
}

main(): Int64 {
    Record7().function26();
    return 0;
}
