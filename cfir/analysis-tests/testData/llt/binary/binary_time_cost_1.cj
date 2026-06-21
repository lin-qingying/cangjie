


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
