// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix

open class A {
    protected var b: Int64 = 1
}

class B <: A {
    public func test() {
        return A().b
    }
}

main(): Int64 {
    if (B().test() == 1) {
        return 0
    }
    return 0
}

/* SCAN
*/ 
