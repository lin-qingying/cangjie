// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

open class C1 {
    private init(a: Int64) {
    }

    public init() {
    }
}

class C2 <: C1 {
    public init() {
    }
}

main() {
    return 0
}
