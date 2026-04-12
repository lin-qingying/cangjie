// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC: %compiler %cmp_opt %compile_lib_opt %n.cj 2>&1 | compare %f
// ASSERT: regex-not core dumped
// ASSERT: regex-not stack trace

main() {
    let err = 1 + true

    try (x = err) {
        let b = 1 + false
    } catch (_: Exception) {
        let b = 1 + false
    } finally {
        let b = 1 + false
    }

    let b: Unit = try (x = err) {
        true
    } catch (_: Exception) {
        let b = 1 + false
    } finally {
        let b = 1 + false
    }

    if (err.isClosed) {}

    0
}

// No errors:
// ASSERT: scan-not :14:
// ASSERT: scan-not :22:
// ASSERT: scan-not :30:

// Errors:
// ASSERT: scan :12:
// ASSERT: scan :15:
// ASSERT: scan :17:
// ASSERT: scan :19:
// ASSERT: scan :25:
// ASSERT: scan :27:
