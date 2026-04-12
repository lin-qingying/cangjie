// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

// Was ICE, should quit normally
struct LA{
  public LA(var ,: Int32) {}
}

main() {}

/* SCAN
error: expected a argument name after '(' in parameter list, found ','
 ==> invalid_primary_ctor.cj:5:17:
  | 
5 |   public LA(var ,: Int32) {}
  |            ~    ^ expected a argument name here
  |            |
  |            after '(' in parameter list
  | 

error: expected a argument name after ',' in parameter list, found ':'
 ==> invalid_primary_ctor.cj:5:18:
  | 
5 |   public LA(var ,: Int32) {}
  |                 ~^ expected a argument name here
  |                 |
  |                 after ',' in parameter list
  | 

error: there should be no return type in a primary constructor
 ==> invalid_primary_ctor.cj:5:20:
  | 
5 |   public LA(var ,: Int32) {}
  |                    ^^^^^ 
  | 

error: body of main constructor 'LA' is missing
 ==> invalid_primary_ctor.cj:5:25:
  | 
5 |   public LA(var ,: Int32) {}
  |                         ^ missing main constructor body here
  | 

4 errors generated, 4 errors printed.
*/


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
