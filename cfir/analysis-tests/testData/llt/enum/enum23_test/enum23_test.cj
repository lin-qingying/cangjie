// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: pkg
// EXEC:%compiler %cmp_opt %compile_lib_opt pkg/pkg.cj -o pkg/pkg.%middle
// EXEC:%compiler %cmp_opt pkg/pkg.%middle --import-path pkg %n.cj -o %output
// EXEC:%run %run_opt %output %run_args

internal import pkg.*

main() {
    let obj = ET<Int64>.A
    return obj.bar(0) + ET<Int64>.f<Bool>(0, true)
}
