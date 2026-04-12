// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

struct R1 {
    public var i = 0
}

extend R1 {
    public mut func foo1() {
        this.i = this.i + 1
        i = i + 1
    }
}

main() {
    var obj = R1()
    obj.foo1()
    if (obj.i == 2) {
        return 0
    }
    return 1
}
