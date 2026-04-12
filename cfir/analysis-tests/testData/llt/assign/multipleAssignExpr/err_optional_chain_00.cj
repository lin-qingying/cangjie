// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class C {}

main() {
    var t: (C, C, C) = (C(), C(), C())
    (t?[1], _) = (C(), 1)
}

/* SCAN
error: cannot use optional chaining
  ==> err_optional_chain_00.cj:13:6:
   | 
13 |     (t?[1], _) = (C(), 1)
   |      ^^^^^ cannot use optional chaining on non-optional value of type 'Tuple<Class-C, Class-C, Class-C>'
   | 

error: mismatched types
  ==> err_optional_chain_00.cj:13:18:
   | 
13 |     (t?[1], _) = (C(), 1)
   |      ~~~~~       ^^^^^^^^ the expression has type 'Tuple<Class-C, Int64>'
   |      |
   |      can not assign it with 'Class-C'
   | 

2 errors generated, 2 errors printed.
*/
