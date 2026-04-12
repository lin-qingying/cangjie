// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %f 2>&1 | compare %f

interface I1 {
    func f1(): Int32
}

interface I2 {
    func f1(): Int32 {
        8
    }
}

class C <: I1 & I2 {
    public func f1(): Int32 {
        2
    }
}

/* SCAN
*/ 
