// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix

class A {
    public mut static prop a: Int64 {
        get() {
            return 4
        }
        set(_) {
            return ()
        }
    }
    public mut prop b: Int64 {
        get() {
            return a
        }
        set(_) {
            return ()
        }
    }
}

main() {
    var a = A()
    A.a = 1
    a.b = 2
    if (a.b == 4) {
        print("${a.b}")
        return 0
    }
    return 1
}

/* SCAN
*/ 
