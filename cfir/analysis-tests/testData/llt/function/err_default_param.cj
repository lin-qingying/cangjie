// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

abstract class A {
    public func f(x!: Bool = true): Unit
}

main() {}

/* SCAN
error: optional parameter cannot be used in abstract function
  ==> err_default_param.cj:10:19:
   | 
10 |     public func f(x!: Bool = true): Unit
   |                   ^^^^^^^^^^^^^^^ 
   | 

1 error generated, 1 error printed.
*/
