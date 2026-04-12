// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

class Base12<T> {
    public var d: T
    public mut prop a: T {
        get() {
            return d
        }
        set(v) {
            d = v
        }
    }
}

main() {
    var x = Base12<Int64>()
    x.a = 4
    if (x.a == 4) {
        print("${x.a}")
        return 0
    }
    return 1
}

/* SCAN
error: the uninitialized member variable 'd' is not initialized in the constructor of class or struct
  ==> property2.cj:10:5:
   | 
10 |     public var d: T
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
