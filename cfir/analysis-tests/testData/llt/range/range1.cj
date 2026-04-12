// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC:%frontendCompiler %typecheck %f 2>&1 | compare %f

var gRange1 = 1..10:2
var gRange2 = ..10
var gRange3 = ..:3
var gRange4 = ..


main() {
    var range1 = 1..10:2
    var range2 = ..10
    var range3 = ..:Int64(3)

    for (i in range1) {
        i
    }

    for i in 1..20{}

    var arr = {1,2,3,4,5}
    var arr1 = arr[1..3]
    var arr2 = arr[..]
    var arr3 = arr[range3]
    return 0
}

// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace
