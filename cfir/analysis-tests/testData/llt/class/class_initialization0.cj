// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix 2>&1 | compare %f

class A <: B {
    public var a = b
}

open class B {
    public var b: Int64 = 1
}

main(): Int64 {
    return 0
}

/* SCAN
error: super member 'b' is not allowed to be used before calling super()
  ==> class_initialization0.cj:10:20:
   | 
10 |     public var a = b
   |                    ^ 
   | 

1 error generated, 1 error printed.
*/
