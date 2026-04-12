// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %frontendCompiler %typecheck %noColor %f 2>&1 | compare %f

main(): Int64 {
    if (B<String>().m != 1) {
        return 1
    }
    return 0
}

interface I<T> {
    func foo(): Int64 {
        return 1
    }
}

class B<T> <: I<T> {}

extend<T> B<T> <: I<T> {
    public prop m: Int64 {
        get() {
            return foo()
        }
    }
}

/* SCAN
error: interface 'Interface-I<Generics-T>' has been implemented by 'Class-B<Generics-T>', please remove it
  ==> extend_duplicate_interfaces6.cj:24:19:
   | 
24 | extend<T> B<T> <: I<T> {
   |                   ^^^^ 
   | 

error: class 'B' inherits or implements duplicate interface 'I'
  ==> extend_duplicate_interfaces6.cj:10:9:
   | 
10 |     if (B<String>().m != 1) {
   |         ^ 
   | 

2 errors generated, 2 errors printed.
*/
