// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

func b(bo!: (Result, Range) -> Bool): Unit {}

/* SCAN
error: undeclared type name 'Result'
 ==> func_param_error.cj:9:14:
  | 
9 | func b(bo!: (Result, Range) -> Bool): Unit {}
  |              ^ 
  | 

error: generic type should be used with type argument
 ==> func_param_error.cj:9:22:
  | 
9 | func b(bo!: (Result, Range) -> Bool): Unit {}
  |                      ^ 
  | 

error: 'main' is missing
 ==> func_param_error.cj:1:1:
  | 
1 | // Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
  | ^ 
  | 

3 errors generated, 3 errors printed.
*/
