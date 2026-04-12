// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class A {
    public var a: Int64 = 1
}

main() {
    A().a<Bool>
    let a1: ?A = A()
    let r1 = a1?.a<Bool>
    return 0
}

/* SCAN
error: type argument's number does not match type parameter's number
  ==> member2.cj:14:5:
   | 
14 |     A().a<Bool>
   |     ^^^^^^^^^^ 
   | 

error: type argument's number does not match type parameter's number
  ==> member2.cj:16:14:
   | 
16 |     let r1 = a1?.a<Bool>
   |              ^^^^^^^^^^ 
   | 

2 errors generated, 2 errors printed.
*/
