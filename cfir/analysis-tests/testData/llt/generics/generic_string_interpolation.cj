// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-PIPE-1: %frontendCompiler %typecheck %cmp_opt %f 2>&1 | compare %f

func fail<T>(instance: T) {
    throw IllegalArgumentException("instance: ${instance}")
}

main() {}

/* SCAN
error: the type 'Generics-T' should implement interface 'ToString' in the 'core' package
  ==> generic_string_interpolation.cj:10:48:
   | 
10 |     throw IllegalArgumentException("instance: ${instance}")
   |                                                ^ 
   | 

1 error generated, 1 error printed.
*/
