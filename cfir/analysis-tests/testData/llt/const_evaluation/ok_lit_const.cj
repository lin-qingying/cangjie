// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %f -o %output
// EXEC: %run %run_opt %output %run_args

const i8 = -8i8
const i16 = -16i16
const i32 = -32i32
const i64 = -64i64
const iNative: IntNative = 0
const u8 = 8u8
const u16 = 16u16
const u32 = 32u32
const u64 = 64u64
const uNative: UIntNative = 0
const f16 = 16.0f16
const f32 = 32.0f32
const f64 = 64.0f64
const unit = ()
const bool = false
const char = '👷'
const string = "中文"

const func test() {
    i8 == -8 && i16 == -16 && i32 == -32 && i64 == -64 && iNative == 0 && u8 == 8 && u16 == 16 && u32 == 32 && u64 == 64 &&
        uNative == 0 && f32 > 31.9 && f32 < 32.1 && f64 > 63.9 && f64 < 64.1 && unit == () && !bool && char == '👷' &&
        string == "中文"
}

main() {
    if (test()) {
        0
    } else {
        1
    }
}
