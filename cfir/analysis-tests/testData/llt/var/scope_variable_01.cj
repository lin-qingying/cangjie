// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f  -o %output
// EXEC: %run %run_opt %output %run_args

// 自增减的变量定义在全局，使用在局部

main(): Int64 {
    var b = foo()
    if (b != 2) {
        return 1
    }
    return 0
}

var res: Int32 = 1

func foo() {
    var a = res
    res++ // error: usage of a variable 'res' which is not defined yet
    res
}
