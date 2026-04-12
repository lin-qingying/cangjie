// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %n.%suffix %run_args | compare %f
// ASSERT: regex-6 true

interface Interface25 {}

interface Interface24 {}

interface Interface21 <: Interface24 & Interface25 {}

interface Interface19 <: Interface21 {}

class Class16 <: Interface19 {}

class Class17 <: Interface24 & Interface25 {}

main() {
    let a: Interface24 = Class17()
    let b1 = [Class16(), Class17(), a]
    let b2 = [Class16(), a, Class17()]
    let b3 = [a, Class16(), Class17()]
    let b4 = [a, Class17(), Class16()]
    let b5 = [Class17(), Class16(), a]
    let b6 = [Class17(), a, Class16()]
    println(b1 is Array<Interface24>)
    println(b2 is Array<Interface24>)
    println(b3 is Array<Interface24>)
    println(b4 is Array<Interface24>)
    println(b5 is Array<Interface24>)
    println(b6 is Array<Interface24>)
    0
}
