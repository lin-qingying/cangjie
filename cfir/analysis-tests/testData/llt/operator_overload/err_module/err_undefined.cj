// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: mod1
// EXEC: %compiler %cmp_opt %compile_lib_opt mod1/pkg.cj -o mod1/pkg.%middle %mod_name_opt mod1
// ERRCHECK: %compiler %cmp_opt mod1/pkg.%middle %f -o %output

internal import mod1.pkg.a

main() {
    let _ = mod1 * a
}

/* SCAN
error: undeclared identifier 'mod1'
  ==> err_undefined.cj:14:13:
   | 
14 |     let _ = mod1 * a
   |             ^ 
   | 

1 error generated, 1 error printed.
*/
