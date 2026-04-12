// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

// Was ICE, should quit normally
package pkgs2
public import public var varpkg2= 20
public import public var varpkg2= 20

main() {}

/* SCAN
error: expected ';' or '<NL>', found keyword 'var'
 ==> invalid_import.cj:5:22:
  | 
5 | public import public var varpkg2= 20
  |                      ^^^ expected ';' or '<NL>' here
  | 

error: expected ';' or '<NL>', found keyword 'var'
 ==> invalid_import.cj:6:22:
  | 
6 | public import public var varpkg2= 20
  |                      ^^^ expected ';' or '<NL>' here
  | 

2 errors generated, 2 errors printed.
*/


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
