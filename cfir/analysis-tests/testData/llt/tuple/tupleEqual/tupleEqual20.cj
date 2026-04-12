// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-1: %compiler %cmp_opt %f -o %output

main(): Int64 {
    var l = (1,1.*)
    var r = (2,2)
    l == r
    return 0
}
