// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %f 2>&1 | compare %f
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

// 自增减的变量定义在全局，使用在局部

main(): Int64 {
    var b = foo()
    if (res != 2) {
        return 1
    }
    return 0
}

var res: Int32 = 1

func foo() {
    var a = res
    res++
    res
}
