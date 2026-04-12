// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE) EXEC: %compiler %f
// (not CJNATIVE) ERRCHECK: %compiler %f %cmp_opt

main() {
    let a = {
        => [VArray<Int64, $3> {_ => 0}]
    }
    a()[0][0]
}

// ASSERT: scan-not Internal Compiler Error
