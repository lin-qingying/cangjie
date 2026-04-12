// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

class A {
    public init(){ (input:Int64):Int64=>{1} }
}

main():Int64 {
    A()
    return 0
}

/* SCAN
error: expected operator or end of expression, found ':'
  ==> class_init_default_constructor9.cj:10:26:
   | 
10 |     public init(){ (input:Int64):Int64=>{1} }
   |                          ^ expected operator or end of expression
   | 

error: unclosed delimiter: '('
  ==> class_init_default_constructor9.cj:10:47:
   | 
10 |     public init(){ (input:Int64):Int64=>{1} }
   |                    ~                          ^ expected ')' here
   |                    |
   |                    to match this opening '('
   | 

error: unexpected main function in function body
  ==> class_init_default_constructor9.cj:13:1:
   | 
10 |     public init(){ (input:Int64):Int64=>{1} }
   |            ~~~~ in function
11 | }
   | ...
13 | main():Int64 {
   | ^^^^ unexpected main function
   | 

error: unclosed delimiter: '{'
  ==> class_init_default_constructor9.cj:16:2:
   | 
10 |     public init(){ (input:Int64):Int64=>{1} }
   |                  ~ to match this opening '{'
11 | }
   | ...
16 | }
   |  ^ expected '}' here
   | 

4 errors generated, 4 errors printed.
*/
