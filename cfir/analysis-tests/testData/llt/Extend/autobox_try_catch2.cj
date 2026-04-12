// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %output | compare %f
// ASSERT: scan In f of extend Int64!

interface I {
    func f() {
        println("In f of interface!");
        return 1
    }
}

extend Int64 <: I {
    public func f() {
        println("In f of extend Int64!");
        return 1
    }
}

func g() {
    let y: I = 2
    let x: I = try {
        y
    } catch (e: Exception) {
        println("Exception caught here")
        9
    } finally {
        11
    }
    x.f()
    return 0
}

main() {
    g()
    return 0
}
