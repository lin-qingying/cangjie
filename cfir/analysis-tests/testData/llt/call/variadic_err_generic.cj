// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

extend<T> Array<T> {
    public func reduce<Tnew>(cb: ((Array<T>) -> T), i: Tnew) {
        var y: Tnew = i
        for (i in 0..this.size) {
            y = cb(this)
        }
        y
    }
}

main() {}

/* SCAN
error: mismatched types
  ==> variadic_err_generic.cj:13:17:
   | 
13 |             y = cb(this)
   |                 ^^^^^^^^ expected 'Generics-Tnew', found 'Generics-T'
   | 

1 error generated, 1 error printed.
*/
