// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

interface I {
    static func test(): Unit
}

abstract class A <: I {}

type B = A

main(): Int64 {
    B.test()
    return 0
}

/* SCAN
error: static invocation contains unimplemented static function 'test'
  ==> class_static_member_access_invalid05.cj:18:7:
   | 
18 |     B.test()
   |       ^^^^ 
   | 

error: abstract class 'A' cannot contain unimplemented static function 'test'
  ==> class_static_member_access_invalid05.cj:13:16:
   | 
13 | abstract class A <: I {}
   |                ^ 
   | 

2 errors generated, 2 errors printed.
*/
