// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f 2>&1 | compare %f

interface I {
    func f(): Int64
}

class C<R> {}

type c<T> = C<T>

extend c {
    public func f1() {
        2
    }
}

main() {
    return 0
}

/* SCAN
error: generic type should be used with type argument
  ==> type_arg_mismatch.cj:17:8:
   | 
17 | extend c {
   |        ^ 
   | 

1 error generated, 1 error printed.
*/
