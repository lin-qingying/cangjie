// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

class A {
    public operator func ==(right: B): Bool {
        return true;
    }
}

class B {
    public operator func ==(right: A): Bool {
        return false;
    }
}

main(): Int64 {
    if ((A(), B()) == (B(), A())) {
        return 1;
    }
    return 0;
}
