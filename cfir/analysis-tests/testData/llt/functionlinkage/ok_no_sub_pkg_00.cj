// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// (CJNATIVE and DEBUG_OPTION) EXEC-PIPE: %frontendCompiler %dump-ir-to-screen --no-sub-pkg %compile_lib_opt --apc=1 %f 2>&1 | compare %f
// (not CJNATIVE and DEBUG_OPTION) EXEC-PIPE: %frontendCompiler %dump-ir-to-screen --no-sub-pkg %compile_lib_opt %f 2>&1 | compare %f
// (CJNATIVE) ASSERT: scan @_CN3pkg11farFarAheadE = global
// (not CJNATIVE) ASSERT: scan @_ZN3pkg11farFarAheadE = global

package pkg

let farAhead = 0
protected let farFarAhead = farAhead
