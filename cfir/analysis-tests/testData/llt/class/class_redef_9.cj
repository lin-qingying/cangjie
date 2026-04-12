// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

open class C1 {
    public static func foo(a: Int64): Int64 {
        return 2
    }
}

class C2 <: C1 {
    public static redef func foo(a: Int64): Int32 {
        return 3
    }
}

main(): Int64 {
    return 1
}

/* SCAN
error: return type of 'foo' is not identical or not a subtype of the overridden/redefined/implement function
  ==> class_redef_9.cj:16:30:
   | 
16 |     public static redef func foo(a: Int64): Int32 {
   |                              ^^^ 
   | 
note: 'Int32' is not a subtype of 'Int64'
  ==> class_redef_9.cj:10:24:
   | 
10 |     public static func foo(a: Int64): Int64 {
   |                        ^^^ 
   | 

1 error generated, 1 error printed.
*/
