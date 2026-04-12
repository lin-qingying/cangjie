// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %f 2>&1 | compare %f

struct RA {
    public let a = 10
    public init(a: Int64) {
        this.a = a
    }
}

main() {
    return 0
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
