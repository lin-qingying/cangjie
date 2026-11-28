package org.cangnova.cangjie.parsing

import com.intellij.openapi.util.Key
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.NotNullablePsiCopyableUserDataProperty

/** 必须在首次读取文件 AST 前写入；PSI 副本与其克隆的语法树共享同一次编译调用的不可变选项。 */
var CjFile.cjmpParsingContext: CjmpParsingContext by
    NotNullablePsiCopyableUserDataProperty(Key.create("CANGJIE_CJMP_PARSING_CONTEXT"), CjmpParsingContext.SyntaxOnly)

/** 宏重解析继承原始文件种类；临时宏文件类型仍单独决定 custom-annotation 入口。 */
var CjFile.parserSourceKind: org.cangnova.cangjie.CjSourceKind?
    get() = getCopyableUserData(PARSER_SOURCE_KIND_KEY)
    internal set(value) { putCopyableUserData(PARSER_SOURCE_KIND_KEY, value) }

private val PARSER_SOURCE_KIND_KEY: Key<org.cangnova.cangjie.CjSourceKind> = Key.create("CANGJIE_PARSER_SOURCE_KIND")

/** 完整 parser 发布的不可变快照随已解析 PSI 树复制；完整重解析会替换副本自己的快照。 */
var CjFile.parsedFileFacts: ParsingFileFacts?
    get() = getCopyableUserData(PARSED_FILE_FACTS_KEY)
    internal set(value) { putCopyableUserData(PARSED_FILE_FACTS_KEY, value) }

private val PARSED_FILE_FACTS_KEY: Key<ParsingFileFacts> = Key.create("CANGJIE_PARSED_FILE_FACTS")

/** 仅已提交的 PSI 根具有此快照；未加载的 stub 不伪造完整解析事件。 */
var CjFile.parsedRoleSnapshot: CjmpFileRoleSyntaxSnapshot?
    get() = getCopyableUserData(PARSED_ROLE_SNAPSHOT_KEY)
    internal set(value) { putCopyableUserData(PARSED_ROLE_SNAPSHOT_KEY, value) }

val CjFile.parsedRoleEvents: List<CjmpParsedRoleEvent>? get() = parsedRoleSnapshot?.events

private val PARSED_ROLE_SNAPSHOT_KEY: Key<CjmpFileRoleSyntaxSnapshot> = Key.create("CANGJIE_PARSED_ROLE_SNAPSHOT")
