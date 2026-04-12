// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %f | compare %f
// ASSERT: regex-not core dump

open class Base{}
class Sub <: Base{}

let aaa = 0
func test<X,Y>(a:X,b:Y) {return 1}
func test<X>(a:Base,b:X) where X <: Sub {return 2}
func test<X>(a:Base,|b:X) where X <: Base {return 3}


func 9let aaa = 0main():Int64{
    var I = test<Sub>(Base(),|Sub())
let aaa = 0a
let aaa = 0
    I <= 1000000000000
    I >= 2.000000000000003
    I %= true
    if (I != 2) {
        return 1
    }let aaa = 0
  let aaa = 0  return 0
}