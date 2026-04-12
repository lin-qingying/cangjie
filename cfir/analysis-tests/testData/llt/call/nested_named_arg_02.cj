// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1:%frontendCompiler %cmp_opt %f 2>&1 | compare %f

internal import std.collection.*

class A {
    A(var run!: () -> Unit = {=>}) {}
}

func foo() {}

var test = ArrayList<A>([A(foo)])

main() {}

/* SCAN
error: missing argument prefix 'run:' for named parameter
  ==> nested_named_arg_02.cj:17:28:
   | 
17 | var test = ArrayList<A>([A(foo)])
   |                            ^^^ 
   | 

1 error generated, 1 error printed.
*/
