// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

struct Base12 {
    public var d: Int64 = 1
    public prop a: Int64 {
        get() {
            return this.d
        }
    }
}

main() {
    var x = Base12()
    if (x.a == 1) {
        print("${x.a}")
        return 0
    }
    return 1
}
