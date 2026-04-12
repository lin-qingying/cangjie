// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC: %run %run_opt %n.%suffix
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

struct R<T> {
    public func foo1(a: T): Int64 {
        return 1
    }
}

extend<T> R<T> {
    public func foo2(b: T): Int64 {
        foo1(b)
        return 2
    }
}

main() {
    var obj = R<Int64>()
    return obj.foo2(1) - 2
}
