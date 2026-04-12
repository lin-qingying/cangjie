// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func test1(v: Any) {}

func test2(v: ToString) {}

main() {
    let x = Some(None)
    test1(None)
    test2(None)
    0
}

/* SCAN
error: generic type should be used with type argument
  ==> enum37.cj:14:18:
   | 
14 |     let x = Some(None)
   |                  ^ 
   | 

error: unable to infer generic argument of this function
  ==> enum37.cj:14:13:
   | 
14 |     let x = Some(None)
   |             ^^^^ 
   | 
note: no information known for type variable 'Generics-T'
  ==> (package std.core)option.cj:11:20:

error: generic type should be used with type argument
  ==> enum37.cj:15:11:
   | 
15 |     test1(None)
   |           ^ 
   | 

error: generic type should be used with type argument
  ==> enum37.cj:16:11:
   | 
16 |     test2(None)
   |           ^ 
   | 

4 errors generated, 4 errors printed.
*/
