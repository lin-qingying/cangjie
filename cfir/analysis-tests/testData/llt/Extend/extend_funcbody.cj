// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

class A{
    public var a = 0
}

extend A {
    public func foo1():Int64 
}

main(): Int64 {
    return A().foo1()
}

/* SCAN
error: body of function 'foo1' is missing
  ==> extend_funcbody.cj:14:29:
   | 
14 |     public func foo1():Int64 
   |                             ^ missing function body here
   | 

1 error generated, 1 error printed.
*/
