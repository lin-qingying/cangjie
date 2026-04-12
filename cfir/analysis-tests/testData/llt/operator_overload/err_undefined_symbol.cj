// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

class A {
    public operator func [](arr: Array<Array<Int64>>) {
        return arr.size - 1
    }
}

main() {
    retur * A()[[1]]
}

/* SCAN
error: undeclared identifier 'retur'
  ==> err_undefined_symbol.cj:16:5:
   | 
16 |     retur * A()[[1]]
   |     ^ 
   | 

1 error generated, 1 error printed.
*/
