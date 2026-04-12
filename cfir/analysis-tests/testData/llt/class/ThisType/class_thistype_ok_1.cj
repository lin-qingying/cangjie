// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output
// EXEC: %run %run_opt %output %run_args
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

class C1 {
    public var x = 0
    public func foo(): This {
        x = 1
        return this
    }
}

main() {
    var obj = C1()
    if (obj.foo().x == 1) {
        return 0
    }
    return 1
}
