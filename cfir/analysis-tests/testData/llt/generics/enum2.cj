// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

enum TimeUnit2<T> where U <: Num { Year2 }
enum TimeUnit2<T> where U <: Num { Year2 }
enum TimeUnit2<T> where U < Num { Year2 } 
enum TimeUnit2<T> where U <: where { Year2 }

/* SCAN
error: expected '<:', found '<'
  ==> enum2.cj:11:27:
   | 
11 | enum TimeUnit2<T> where U < Num { Year2 } 
   |                           ^ expected '<:' here
   | 

error: expected type name after '<:', found keyword 'where'
  ==> enum2.cj:12:30:
   | 
12 | enum TimeUnit2<T> where U <: where { Year2 }
   |                           ~~ ^^^^^ expected type name here
   |                           |
   |                           after '<:'
   | 

2 errors generated, 2 errors printed.
*/
