// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (not Windows)EXEC:%frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

interface I<T> {}

struct B<T> where T <: I<T> {
    init() {}
}

func test1<T>() where T <: I<T> {
    // test for error report for join of return type
    if (true) {
        return B<Int64>()
    } else if (false) {
        return 1
    } else {
        return ()
    }
}

func test2<T>(): Unit where T <: I<T> {
    // test for error report for join of return type and check
    if (true) {
        return B<Int64>()
    } else if (false) {
        return 1
    } else {
        return ()
    }
}

extend Int64 <: I<Int64> {}

main() {
    test1<Int64>()
    return 0
}

/* SCAN
error: mismatched types
  ==> infer_multiple_return.cj:29:16:
   | 
29 |         return B<Int64>()
   |                ^^^^^^^^^^ expected 'Unit', found 'Struct-B<Int64>'
   | 

error: cannot convert an integer literal to type 'Unit'
  ==> infer_multiple_return.cj:31:16:
   | 
31 |         return 1
   |                ^ 
   | 

2 errors generated, 2 errors printed.
*/
