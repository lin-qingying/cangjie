// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

struct R{
    public let r : Int32
    public init(x:Int32) {
        r = x
    }
    public operator func ++():R{       
        return R(this.r + 1)
    }
}

main():Int64 {
    var r:R = R(-1)
    var r1: R = r++
    return Int64(r1.r)
}

/* SCAN
error: cannot overload operator '++' 
  ==> incordec14.cj:14:26:
   | 
14 |     public operator func ++():R{       
   |                          ^^ cannot overload this operator
   | 
   # note: only operator '+', '-', ... can be overloaded

1 error generated, 1 error printed.
*/
