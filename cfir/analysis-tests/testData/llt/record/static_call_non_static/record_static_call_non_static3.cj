// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dump
// ASSERT: regex-not stack trace

let var7: Record5 = Record5()

public struct Record5 {
    var var25: Bool = true;

    func function10(): Bool {
        if (!false) {
            var7.var25
        } else {
            false
        };
    }
}

main(): Int64 {
    if (!var7.function10()) {
        return 1
    }
    return 0
}
