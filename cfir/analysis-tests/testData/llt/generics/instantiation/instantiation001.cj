// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

main(): Int64 {
    if (foo2().get(2) != 1) {
        print("${foo2().get(2)}")
        return 1
    }
    return 0
}

func foo2(): List<Int64> {
    var a = List<Int64>([1, 2, 3])
    return a
}
