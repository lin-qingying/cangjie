// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

public interface I1 {
    static func foo1(): Unit
}

public interface I2 <: I1 {
    static func foo2() {
        foo1()
    }
}

main() {
    I2.foo2()
    return 1
}

/* SCAN
error: static invocation contains unimplemented static function 'foo1'
  ==> static_default_impl_call_non_impl.cj:20:5:
   | 
20 |     I2.foo2()
   |     ^^^^^^^ 
   | 
note: indirect use of unimplemented function reference here
  ==> static_default_impl_call_non_impl.cj:15:9:
   | 
15 |         foo1()
   |         ^^^^ 
   | 

1 error generated, 1 error printed.
*/
