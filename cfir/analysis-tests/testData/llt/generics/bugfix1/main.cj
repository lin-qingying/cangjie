// DEPENDENCE: pkg.cj
// EXEC: %compiler %cmp_opt %compile_lib_opt pkg.cj -o libpkg.%lib_extension
// EXEC: %compiler %f libpkg.%lib_extension
// ASSERT: scan-not core dumped

import pkg.*

main() {
    let _ = CA<Float32>()
}


// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.
