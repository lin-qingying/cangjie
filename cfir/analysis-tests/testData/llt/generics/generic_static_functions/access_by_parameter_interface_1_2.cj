// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %cmp_opt %typecheck %cmp_opt %f 2>&1 | compare %f

interface I {
    let a = 0    
}

func Foo<U>(a:U) where U <: I {
    var b = U.a
    return b
}

class C1 <: I {}

main() {
    return Foo<C1>(C1())
}

/* SCAN
[33mwarning[0m: '--error-count-limit' is specified multiple times. The last one is chosen.
[33mwarning[0m: '--diagnostic-format' is specified multiple times. The last one is chosen.
error: unexpected variable declaration in interface body
  ==> access_by_parameter_interface_1_2.cj:10:5:
   | 
9  | interface I {
   | ~~~~~~~~~ in interface
10 |     let a = 0    
   |     ^^^ unexpected variable declaration
   | 

1 error generated, 1 error printed.
*/
