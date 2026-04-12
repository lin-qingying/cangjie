// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f

class A <: Resource {
    public func isClosed() {
        true
    }
    public func close() {}
}

func f() {}

func f(_: Unit) {}

func g<T>(_: Unit) {}

main() {
    f(try (x = A()) {} finally {
        let _ = A(1)
    }) |> g<Unit>
}

/* SCAN
error: extra argument given for parameter list '()' in call
  ==> pipeline9.cj:24:18:
   | 
24 |         let _ = A(1)
   |                  ^^^ expected 0 arguments, found 1
   | 

1 error generated, 1 error printed.
*/
