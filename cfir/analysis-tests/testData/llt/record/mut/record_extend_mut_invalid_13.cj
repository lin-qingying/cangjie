// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %n.%suffix 2>&1 | compare %f

main() {
    var b = R(a: 6)
    b.c(7)
    return Int64(b.a - 7)
}

struct R {
    public var a: Int32 = 8
    public func cc() {
        c(1)
    }
}

extend R {
    public mut func c(b: Int32): Int32 {
        return 0
    }
}

/* SCAN
error: extra argument given for parameter list '()' in call
  ==> record_extend_mut_invalid_13.cj:10:14:
   | 
10 |     var b = R(a: 6)
   |              ^^^^^^ expected 0 arguments, found 1
   | 

error: undeclared identifier 'c'
  ==> record_extend_mut_invalid_13.cj:18:9:
   | 
18 |         c(1)
   |         ^ 
   | 

2 errors generated, 2 errors printed.
*/
