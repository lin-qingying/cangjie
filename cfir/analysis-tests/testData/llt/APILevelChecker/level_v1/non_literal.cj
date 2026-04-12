// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: apilevel.cj
// EXEC: %compiler %cmp_opt %diag_json %compile_lib_opt apilevel.cj -o apilevel.%middle
// ERRCHECK: %compiler %cmp_opt apilevel.%middle %n.cj -o %n.%suffix
// ASSERT: scan 
// ASSERT: scan 

import ohos.labels.*

const a: UInt8 = 10
const b = "xxx"

@!APILevel[a, syscap: b]
class A {
    public A() {}
}

main() {}
