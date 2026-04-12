// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

abstract class A {
    protected open func f(): Int64
}

abstract class B <: A {
    protected override func f(): Int64
}

class C <: B {
    protected override func f(): Int64 {
        return 1
    }
}

main() {
    return C().f() - 1
}
