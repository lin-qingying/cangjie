// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %compile_lib_opt %n.cj -o %output 2>&1 | compare %f

class A {}

main() {
    try (input = A()) {
    } catch (e: Exception) {
        return 1
    }
    try (aaa = ()) {
        aaa;
        ()
    }
    return 0
}

/* SCAN
error: mismatched types
  ==> try_with_resource_n.cj:12:10:
   | 
12 |     try (input = A()) {
   |          ^^^^^^^^^^^ expected 'Interface-Resource', found 'Class-A'
   | 
   # note: the resource specification should implement interface 'Resource'

error: mismatched types
  ==> try_with_resource_n.cj:16:10:
   | 
16 |     try (aaa = ()) {
   |          ^^^^^^^^ expected 'Interface-Resource', found 'Unit'
   | 
   # note: the resource specification should implement interface 'Resource'

2 errors generated, 2 errors printed.
*/
