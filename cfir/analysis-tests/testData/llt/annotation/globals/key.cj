// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// DEPENDENCE: apilevel.cj
// EXEC: %compiler apilevel.cj %compile_lib_opt -o liba.%lib_extension
// EXEC: %compiler %f liba.%lib_extension

import a.*

@APILevel[18, stagemodeonly: true, syscap: "1"]
public let MAX_KEY_LENGTH = 1024

main() {}
