// Copyright (c) Huawei Technologies Co., Ltd. 2025. All rights reserved.
// This source file is part of the Cangjie project, licensed under Apache-2.0
// with Runtime Library Exception.
//
// See https://cangjie-lang.cn/pages/LICENSE for license information.

// EXEC-1:%frontendCompiler %typecheck %cmp_opt %f
// TIMEOUT: 5

func yamlParserFetchNextToken(parser: YamlParserT): Bool {
    if (!(isBlankz(parser.buffer, parser.bufferPos) || parser.buffer[parser.bufferPos] == '-' ||
        parser.buffer[parser.bufferPos] == '?' || parser.buffer[parser.bufferPos] == ':' ||
        parser.buffer[parser.bufferPos] == ',' || parser.buffer[parser.bufferPos] == '[' ||
        parser.buffer[parser.bufferPos] == ']' || parser.buffer[parser.bufferPos] == '{' ||
        parser.buffer[parser.bufferPos] == '}' || parser.buffer[parser.bufferPos] == '#' ||
        parser.buffer[parser.bufferPos] == '&' || parser.buffer[parser.bufferPos] == '*' ||
        parser.buffer[parser.bufferPos] == '!' || parser.buffer[parser.bufferPos] == '|' ||
        parser.buffer[parser.bufferPos] == '>' || parser.buffer[parser.bufferPos] == '\'' ||
        parser.buffer[parser.bufferPos] == '"' || parser.buffer[parser.bufferPos] == '%' ||
        parser.buffer[parser.bufferPos] == '@' || parser.buffer[parser.bufferPos] == '`') || (parser.buffer[parser
            .bufferPos] == '-' && !isBlank(parser.buffer, parser.bufferPos + 1)) || (parser.flowLevel == 0 && (parser
        .buffer[parser.bufferPos] == '?' || parser.buffer[parser.bufferPos] == ':') &&
        !isBlankz(parser.buffer, parser.bufferPos + 1))) {
        return yamlParserFetchPlainScalar(parser)
    }

    // If we don't determine the token type so far, it is an error.
    return yamlParserSetScannerError(
        parser,
        "while scanning for the next token",
        parser.mark,
        "found character that cannot start any token"
    )
}
