// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %compiler %cmp_opt %f -o %output 2>&1 | compare %f
// (not CJNATIVE) ASSERT: scan the number '9223372036854775808' exceeds the value range of type 'Int32'
// (CJNATIVE) ASSERT: scan the number '9223372036854775808' exceeds the value range of type 'Int64'

main(): Int64 {
    var varr: VArray<Int64, $9223372036854775808> = VArray<Int64, $9223372036854775808> {i => i + 1}
    return varr[0] - 1
}
