// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

// Was ICE, should quit normally
main() {
    try () /he/ no a re.
    {
    } finally {}
}

/* SCAN
error: expected a name after this, found ')'
 ==> invalid_try_with_resource.cj:5:10:
  | 
5 |     try () /he/ no a re.
  |         ~^ expected a name here
  |         |
  |         after this
  | 

error: expected '{', found '/'
 ==> invalid_try_with_resource.cj:5:12:
  | 
5 |     try () /he/ no a re.
  |            ^ expected '{' here
  | 

error: expected ';' or '<NL>', found 'a'
 ==> invalid_try_with_resource.cj:5:20:
  | 
5 |     try () /he/ no a re.
  |                    ^ expected ';' or '<NL>' here
  | 

error: expected '=>' in lambda expression, found '}'
 ==> invalid_try_with_resource.cj:7:5:
  | 
7 |     } finally {}
  |     ^ expected '=>' here
  | 

error: expected ';' or '<NL>', found keyword 'finally'
 ==> invalid_try_with_resource.cj:7:7:
  | 
7 |     } finally {}
  |       ^^^^^^^ expected ';' or '<NL>' here
  | 

error: expected expression or declaration, found '}'
 ==> invalid_try_with_resource.cj:8:1:
  | 
8 | }
  | ^ expected expression or declaration here
  | 

6 errors generated, 6 errors printed.
*/


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
