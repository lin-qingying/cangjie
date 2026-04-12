// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

class a {
  func isClosed }
         func ({
  try     (b a

/* SCAN
error: expected '(' or '<', found '}'
  ==> funcDecl5.cj:10:17:
   | 
10 |   func isClosed }
   |                 ^ expected '(' or '<' here
   | 

error: expected a func name after keyword 'func', found '('
  ==> funcDecl5.cj:11:15:
   | 
11 |          func ({
   |          ~~~~ ^ expected a func name here
   |          |
   |          after keyword 'func'
   | 

error: expected a argument name after '(' in parameter list, found '{'
  ==> funcDecl5.cj:11:16:
   | 
11 |          func ({
   |               ~^ expected a argument name here
   |               |
   |               after '(' in parameter list
   | 

error: expected '=', found 'a'
  ==> funcDecl5.cj:12:14:
   | 
12 |   try     (b a
   |              ^ expected '=' here
   | 

error: expected '{', found '<EOF>'
  ==> funcDecl5.cj:12:15:
   | 
12 |   try     (b a
   |               ^ expected '{' here
   | 

error: unclosed delimiter: '('
  ==> funcDecl5.cj:12:15:
   | 
12 |   try     (b a
   |           ~   ^ expected ')' here
   |           |
   |           to match this opening '('
   | 

6 errors generated, 6 errors printed.
*/
