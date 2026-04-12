// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// ERRCHECK: %compiler %diag_json %f -o %output

class B<K, L> where K <: (B<L, L>) -> Unit {}

main() {}

// ASSERT: scan-1 sema_generic_type_argument_not_match_constraint
// ASSERT: scan-1 sema_upper_bound_must_be_class_or_interface
// ASSERT: scan "Errors": 2
