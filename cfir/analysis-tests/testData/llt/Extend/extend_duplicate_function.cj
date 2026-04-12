// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I<T> {
    func foo(): Int64 {
        return 1
    }
}

class B<T> <: I<String> {}

extend<T> B<T> <: I<Int64> {
    public func foo(): Int64 {
        return 2
    }
}

main(): Int64 {
    return 0
}

/* SCAN
error: extend member 'foo' is not allowed to shadow members of 'Class-B<Generics-T>'
  ==> extend_duplicate_function.cj:18:17:
   | 
18 |     public func foo(): Int64 {
   |                 ^^^ 
   | 
note: shadowed definition of 'foo' is here
  ==> extend_duplicate_function.cj:10:10:
   | 
10 |     func foo(): Int64 {
   |          ^^^ 
   | 

1 error generated, 1 error printed.
*/
