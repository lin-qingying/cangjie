// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

open class A {
    public operator func ==(right: A): Bool {
        return true;
    }
}

class B <: A {}

main(): Int64 {
    let x = (A(), 1, B());
    if (x == (B(), 2, A())) {
        return 2;
    }
    if (x == (B(), 1, A())) {
        return 0;
    }
    return 1;
}
