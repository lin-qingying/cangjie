// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

interface I {
    static func f1(): Unit
}

abstract class AbsC1 <: I {}

main() {
    AbsC1.f1()
}

/* SCAN
error: static invocation contains unimplemented static function 'f1'
  ==> abstract_class_static_defalt_impl.cj:16:11:
   | 
16 |     AbsC1.f1()
   |           ^^ 
   | 

error: abstract class 'AbsC1' cannot contain unimplemented static function 'f1'
  ==> abstract_class_static_defalt_impl.cj:13:16:
   | 
13 | abstract class AbsC1 <: I {}
   |                ^^^^^ 
   | 

2 errors generated, 2 errors printed.
*/
