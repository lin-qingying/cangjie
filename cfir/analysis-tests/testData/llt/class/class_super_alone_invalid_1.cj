// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %n.cj -o %output 2>&1 |compare %f

open class C0 {}

open class C1 <: C0 {
    public var x = super
}

main(): Int64 {
    return 1
}

/* SCAN
error: invalid super expression, it can only be used on the left-hand side of a dot
  ==> class_super_alone_invalid_1.cj:12:20:
   | 
12 |     public var x = super
   |                    ^ 
   | 

1 error generated, 1 error printed.
*/
