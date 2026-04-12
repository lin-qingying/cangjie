// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %n.%suffix
// EXEC-1: %run %run_opt %n.%suffix

main() {
    var x: Array<UInt8> = Array(0) {i => 8}
    let a = Array(1, repeat: 2)
    a.size
}
