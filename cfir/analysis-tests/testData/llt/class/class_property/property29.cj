// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

main(): Int64 {
    var x = Boo()
    if (x.a != 2) {
        return 1
    }
    if (x.b != 1) {
        return 2
    }
    if (x.c != ()) {
        return 3
    }
    return 0
}

struct Boo {
    public var a: Int32 = 1 // a = 1
    public mut prop m: Int32 {
        get() {
            return a
        }
        set(input) {
            a = input + 1
        }
    }
    public prop n: Int32 {
        get() {
            return a
        }
    }
    public var b: Int32 = if (true) {
        a
    } else {
        0
    }
    // a = 1,b = 1
    public var c: Unit = if (true) {
        m = 1
    }
}

/* SCAN
error: 'm' is not allowed to be accessed before all member variables are initialized
  ==> property29.cj:45:9:
   | 
45 |         m = 1
   |         ^ 
   | 

1 error generated, 1 error printed.
*/
