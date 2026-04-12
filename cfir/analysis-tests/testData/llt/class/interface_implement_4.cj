// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I1 {
    func foo(): Int64 {
        return 1
    }
}

interface I2 {
    func foo(): Int64 {
        return 2
    }
}

open class C1 <: I1 & I2 {}

open class C2 <: I1 & I2 {
    public func foo(): Int64 {
        return 3
    }
}

main(): Int64 {
    var res = C1().foo()
    if (res == 1) {
        return 0
    }
    return 1
}

/* SCAN
error: ambiguous match for function call 'foo'
  ==> interface_implement_4.cj:30:20:
   | 
30 |     var res = C1().foo()
   |                    ^ 
   | 
note: found candidate
  ==> interface_implement_4.cj:10:10:
   | 
10 |     func foo(): Int64 {
   |          ^ 
   | 
note: found candidate
  ==> interface_implement_4.cj:16:10:
   | 
16 |     func foo(): Int64 {
   |          ^ 
   | 

error: interface function 'foo' must be implemented in 'C1'
  ==> interface_implement_4.cj:21:1:
   | 
21 | open class C1 <: I1 & I2 {}
   | ^ 
   | 

2 errors generated, 2 errors printed.
*/
