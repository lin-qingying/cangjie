// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

class A {
    public var x: Int64 = 0
    public mut prop X: Int64 {
        get() {
            return x + y
        }
        set(v) {
            x = v + y
        }
    }
    public static var y: Int64 = 0
    public mut static prop Y: Int64 {
        get() {
            return y
        }
        set(v) {
            X = v
        }
    }
}

main() {
    return 0
}

/* SCAN
error: 'X' is non-static member, cannot be accessed by static function 'set'
  ==> property_call_03.cj:25:13:
   | 
25 |             X = v
   |             ^ 
   | 

1 error generated, 1 error printed.
*/
