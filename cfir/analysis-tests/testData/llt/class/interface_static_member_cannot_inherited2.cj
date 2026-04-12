// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %cmp_opt %typecheck %f 2>&1 | compare %f

interface A {
    let a: Int64 = 1 
}
class B <: A{
}
main():Int64 {
    var b: Int64 = B().a
    return 0
}

/* SCAN
error: unexpected variable declaration in interface body
  ==> interface_static_member_cannot_inherited2.cj:10:5:
   | 
9  | interface A {
   | ~~~~~~~~~ in interface
10 |     let a: Int64 = 1 
   |     ^^^ unexpected variable declaration
   | 

1 error generated, 1 error printed.
*/
