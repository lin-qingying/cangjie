// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f
// EXEC: %run %run_opt %n.%suffix

class A {
    public var x: Int64 = 2
    public mut prop b: Int64 {
        get() {
            return x
        }
        set(v) {
            x = v
            return ()
        }
    }
}

main() {
    var a = A()
    a.b += 5
    a.b *= 5
    if (a.b == 35) {
        print("${a.b}\n")
        return 0
    }
    return 1
}

/* SCAN
*/ 
