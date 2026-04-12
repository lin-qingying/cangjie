// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

open class A {
    public static var a: Int64 = 1
}

class B <: A {}

main(): Int64 {
    var b: Int64 = B().a
    B.a
    return 0
}

/* SCAN
error: object cannot access static member 'a'
  ==> class_static_member_access_invalid.cj:16:20:
   | 
16 |     var b: Int64 = B().a
   |                    ^ 
   | 

1 error generated, 1 error printed.
*/
