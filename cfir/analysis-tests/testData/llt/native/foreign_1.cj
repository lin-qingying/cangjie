// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

foreign func asserts<T>(input:T):Int32

/* SCAN
error: foreign function should not be generic function
 ==> foreign_1.cj:9:21:
  | 
9 | foreign func asserts<T>(input:T):Int32
  |                     ^^^ 
  | 

1 error generated, 1 error printed.
*/
