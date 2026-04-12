// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

interface I {
    static func f(): Unit
    static func f1(): Unit {
        func f2(): Unit {
            f()
        }
        f2()
    }
}

main() {
    I.f1()
}

/* SCAN
error: static invocation contains unimplemented static function 'f'
  ==> interface_static_call_embedded_func.cj:20:5:
   | 
20 |     I.f1()
   |     ^^^^ 
   | 
note: indirect use of unimplemented function reference here
  ==> interface_static_call_embedded_func.cj:13:13:
   | 
13 |             f()
   |             ^ 
   | 

1 error generated, 1 error printed.
*/
