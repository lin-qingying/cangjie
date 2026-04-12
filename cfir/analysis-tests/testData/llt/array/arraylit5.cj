// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

class A {}

class B {}

main() {
    var arr1 = [1, A(), true]
    var arr2 = [A(), B()]
    let x: Object = arr2[0]
    if (arr1[0] is Int64 && arr1[1] is A && arr1[2] is Bool) {
        return 0
    }
    return 1
}
