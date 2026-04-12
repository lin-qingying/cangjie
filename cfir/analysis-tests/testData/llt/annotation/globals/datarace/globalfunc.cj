// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// LEVEL: 2
// datarace testcase, run only under tsan

// DEPENDENCE: ../apilevel.cj
// EXEC: %compiler apilevel.cj %compile_lib_opt -o liba.%lib_extension
// (Linux) EXEC: for i in $(seq 200); do echo "@APILevel[0] public func foo$i() {}"; done >> %f
// EXEC: %compiler %f liba.%lib_extension

import a.*

main() {}
