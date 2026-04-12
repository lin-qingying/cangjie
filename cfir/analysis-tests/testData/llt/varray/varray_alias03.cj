// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f  -o %output
// RUN-EXEC-PIPE-0: %run %run_opt %output %run_args

public type int128 = VArray<UInt8, $16>

main() {
    var a = int128(repeat: 0)
    var b = VArray<UInt8, $16>(repeat: 0)
}
