// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f
// ASSERT: scan 'this' is not allowed to be used in arguments of constructor call

class A {
    public init() {
        this
    }
    public func get(a: A) {
        1
    }
    public init(a: Int64) {
        get(this)
    }
    public init(a: Bool) {
        this(this)
    }
    public init(a: A) {}
}
