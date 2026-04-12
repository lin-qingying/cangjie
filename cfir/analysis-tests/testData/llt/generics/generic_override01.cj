// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface I1 {
    func foo(): Int64
}

interface I3 {
    func goo(): I1
}

class CA<T> <: I3 where T <: I1 {
    var x: T
    init(_x: T) {
        x = _x
    }
    public func goo(): T { // '()->T' cannot override '()->I1' even if 'T <: I1', function signature mast be '()->I1'.
        println("here")
        x
    }
}

struct TEST_1 <: I1 {
    public func foo(): Int64 {
        0
    }
}

main() {
    0
}

/* SCAN
error: return type of 'goo' can only be class/interface type which implements or inherits the interface type 'Interface-I1'
  ==> generic_override01.cj:22:5:
   | 
22 |     public func goo(): T { // '()->T' cannot override '()->I1' even if 'T <: I1', function signature mast be '()->I1'.
   |     ^ 
   | 
note: cannot override/implement the following function
  ==> generic_override01.cj:14:5:
   | 
14 |     func goo(): I1
   |     ^^^^^^^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
