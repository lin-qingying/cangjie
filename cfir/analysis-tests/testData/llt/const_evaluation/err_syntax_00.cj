// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

var this : Int64 = 1

main():Int64{
    var Int8 : Int64 = 1
    const func : Int64 = 1
    return 1
}

/* SCAN
error: expected identifier or pattern after 'var', found keyword 'this'
 ==> err_syntax_00.cj:9:5:
  | 
9 | var this : Int64 = 1
  | ~~~ ^^^^ expected identifier or pattern here
  | 
help: escape the keyword `this` to use it as an identifier:
  | 
9 | var `this` : Int64 = 1
  |     ~~~~~~ 
  | 

error: expected identifier or pattern after 'var', found keyword 'Int8'
  ==> err_syntax_00.cj:12:9:
   | 
12 |     var Int8 : Int64 = 1
   |     ~~~ ^^^^ expected identifier or pattern here
   | 
help: escape the keyword `Int8` to use it as an identifier:
   | 
12 |     var `Int8` : Int64 = 1
   |         ~~~~~~ 
   | 

error: expected a func name after keyword 'func', found ':'
  ==> err_syntax_00.cj:13:16:
   | 
13 |     const func : Int64 = 1
   |           ~~~~ ^ expected a func name here
   |           |
   |           after keyword 'func'
   | 

error: expected ';' or '<NL>', found '='
  ==> err_syntax_00.cj:13:24:
   | 
13 |     const func : Int64 = 1
   |                        ^ expected ';' or '<NL>' here
   | 

4 errors generated, 4 errors printed.
*/
