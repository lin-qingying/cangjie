// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: %n.pkg.cj
// EXEC: %compiler %n.pkg.cj %compile_lib_opt -o lib%n.%lib_extension
// ERRCHECK: %compiler %f lib%n.%lib_extension %cmp_opt

import a.*

@F
extend Int8 {}

@F
func foo() {}

@F
public var a = 3

main() {
    foo()
    0
}

/* SCAN
error: '@F' not applicable to extend
  ==> 255.cj:13:1:
   | 
13 | @F
   | ^^ 
   | 

error: '@F' not applicable to global variable
  ==> 255.cj:19:1:
   | 
19 | @F
   | ^^ 
   | 

2 errors generated, 2 errors printed.
*/
