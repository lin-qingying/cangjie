// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

open class A {
    public var s1: UInt32 = 1
}

class B <: A {
    public var b1: UInt32 = 2
    public var obj: A = B()
}

main() {
    var c: B = B()
    0
}

/* SCAN
error: recursive constructor calling detected
  ==> recursive_ctor_03.cj:15:25:
   | 
15 |     public var obj: A = B()
   |                         ^ 
   | 
note: depends on
  ==> recursive_ctor_03.cj:13:7:
   | 
13 | class B <: A {
   |       ^ 
   | 
note: depends on
  ==> recursive_ctor_03.cj:15:25:
   | 
15 |     public var obj: A = B()
   |                         ^ 
   | 

1 error generated, 1 error printed.
*/
