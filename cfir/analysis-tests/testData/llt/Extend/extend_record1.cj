// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix

struct R {
    public func add(): Int64 {
        return 1
    }
}

extend R {
    public func foo(): Int64 {
        add()
    }
}

main() {
    let a = R();
    return a.foo() - 1
}
