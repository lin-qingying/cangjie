// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg.cj
// EXEC: mkdir -p ohos
// EXEC: %compiler --module-name ohos %cmp_opt %compile_lib_opt pkg.cj -o ./ohos/pkg.%middle
// EXEC: %compiler %cmp_opt ./ohos/pkg.%middle %n.cj -o %n.%suffix

internal import ohos.base.*

main() {
    let fut0 = spawn (MainThreadContext()) {
        println("hello world")
    }
    let fut1 = spawn (Main) {
        println("hello world")
    }
    return 0
}
