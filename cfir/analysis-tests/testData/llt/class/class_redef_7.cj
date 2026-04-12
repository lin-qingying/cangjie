// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

open class C1 {
    public static func foo(): Int64 {
        return 2
    }
}

class C2 <: C1 {
    public static redef func foo1(): Int64 {
        return 3
    }
}

main(): Int64 {
    return 1
}

/* SCAN
error: 'redef' function 'foo1' does not have a redefined 'static' function in its supertype
  ==> class_redef_7.cj:16:5:
   | 
16 |     public static redef func foo1(): Int64 {
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
