// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

interface I {
    static func f1(): Unit
}

main() {
    I.f1() // error, cannot directly call
}

/* SCAN
error: static invocation contains unimplemented static function 'f1'
  ==> static_member_ass01.cj:14:7:
   | 
14 |     I.f1() // error, cannot directly call
   |       ^^ 
   | 

1 error generated, 1 error printed.
*/
