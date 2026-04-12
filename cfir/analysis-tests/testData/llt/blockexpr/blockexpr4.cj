// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

struct Record8 {
    public var var274: Bool = true;
}

func function38(param39: Int32, param40: Int32): Bool {
    {
        => Record8();
    }().var274;
}

main(): Int64 {
    return 0;
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
