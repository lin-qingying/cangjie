// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args 2>&1 | compare %f
// ASSERT: scan-2 foo

interface I1 {
    func foo() {
        println("foo")
    }
}

interface I2 <: I1 {}

class A {}

extend A <: I1 {}

extend A <: I2 {} // A 中已存在 foo，实现子接口的扩展不再拷贝 foo 。

class B {}

extend B <: I2 {}

extend B <: I1 {}

main() {
    A().foo()
    B().foo()
}
