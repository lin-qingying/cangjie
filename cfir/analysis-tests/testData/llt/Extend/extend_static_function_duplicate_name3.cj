// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %noColor %typecheck %f 2>&1 | compare %f

class A {
    public var a = 0
    public static func foo1() {
        return 0
    }
}

extend A {
    public static func foo1() {
        return 0
    }
}

extend A {
    public static func foo1(a: Int32) {
        return 0
    }
}

main(): Int64 {
    return 0
}

/* SCAN
error: extend member 'foo1' is not allowed to shadow members of 'Class-A'
  ==> extend_static_function_duplicate_name3.cj:17:24:
   | 
17 |     public static func foo1() {
   |                        ^^^^ 
   | 
note: shadowed definition of 'foo1' is here
  ==> extend_static_function_duplicate_name3.cj:11:24:
   | 
11 |     public static func foo1() {
   |                        ^^^^ 
   | 

1 error generated, 1 error printed.
*/
