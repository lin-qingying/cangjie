// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.


main(): Int64 {
    let b = [[1u8, 2u8, 3u8]]
    <!PARAMETERS_AND_ARGUMENTS_MISMATCH!>b<!>[<!CANNOT_CONVERT_LITERAL!>1<!>.01][0] != 1
    return 0
}
