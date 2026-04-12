// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %cmp_opt %f -o %output

main(): Int64 {
    return run()
}

func run(): Int64 {
    var res1 = Data().Poo()
    var res2 = Data().Qoo()
    if (res1 == 4 && res2 == 6) {
        return 0
    }
    return 1
}

class Data <: Base {
    public func Poo(a!: Int64 = 4) {
        return a
    }

    public func Qoo(a!: Int64 = 6) {
        return a
    }
}

abstract class Base {
    public func Poo(a!: Int64 = 2): Int64
    public func Qoo(a!: Int64 = 3): Int64
}

/* SCAN
error: optional parameter cannot be used in abstract function
  ==> defaultParameter1.cj:33:21:
   | 
33 |     public func Poo(a!: Int64 = 2): Int64
   |                     ^^^^^^^^^^^^^ 
   | 

error: optional parameter cannot be used in abstract function
  ==> defaultParameter1.cj:34:21:
   | 
34 |     public func Qoo(a!: Int64 = 3): Int64
   |                     ^^^^^^^^^^^^^ 
   | 

2 errors generated, 2 errors printed.
*/
