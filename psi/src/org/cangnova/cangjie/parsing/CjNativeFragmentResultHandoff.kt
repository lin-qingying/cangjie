package org.cangnova.cangjie.parsing

import com.intellij.lang.ASTNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.pom.PomModel
import com.intellij.pom.PomModelAspect
import com.intellij.pom.event.PomModelEvent
import com.intellij.pom.event.PomModelListener
import com.intellij.pom.tree.TreeAspect
import com.intellij.pom.tree.events.TreeChangeEvent
import com.intellij.psi.impl.source.CharTableImpl
import com.intellij.psi.impl.source.tree.FileElement
import com.intellij.psi.impl.source.tree.ICodeFragmentElementType
import com.intellij.psi.impl.source.tree.SharedImplUtil
import com.intellij.util.CharTable
import com.intellij.util.containers.CollectionFactory
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNodeTypes
import org.cangnova.cangjie.psi.parsedLanguageModuleName

/**
 * 原生片段的真实 AST 事务交接。SDK 整文件替换搬运 children 和原生 CharTable，不复制外壳数据；
 * 每个新 native 根先建立独占表，冻结结果按表对象身份登记，零孩子提交走同一协议。
 */
internal object CjNativeFragmentResultHandoff {
    private val ROOT_TABLE_KEY: Key<CharTable> = Key.create("CANGJIE_NATIVE_FRAGMENT_TABLE")
    private val REGISTRY_KEY: Key<Registry> = Key.create("CANGJIE_PARSING_RESULT_HANDOFF")
    private val registryLock = Any()

    /** 必须早于 PsiBuilderImpl 构造时捕获 effective CharTable；不包装原生表或修改旧候选对象。 */
    fun prepareRoot(project: Project, root: ASTNode): ASTNode {
        registry(project)
        if (root.elementType !is ICodeFragmentElementType) return root
        check(root is FileElement) { "A native fragment parser requires the actual native FileElement." }
        val created = CharTableImpl()
        root.setCharTable(created)
        root.putUserData(ROOT_TABLE_KEY, created)
        val actual = checkNotNull(root.getUserData(ROOT_TABLE_KEY))
        check(root.charTable === actual && SharedImplUtil.findCharTableByTree(root) === actual)
        return root
    }

    /** 一张本事务原生表只对应一个实际 producer；值不反向持有表或候选 AST。 */
    fun registerFrozenResult(project: Project, root: ASTNode, result: CjParsedFileResult) {
        if (root.elementType !is ICodeFragmentElementType) return
        check(root is FileElement)
        val table = checkNotNull(root.getUserData(ROOT_TABLE_KEY)) { "The builder must prepare its native table before parsing." }
        check(root.charTable === table && SharedImplUtil.findCharTableByTree(root) === table)
        registry(project).register(table, RootResult(root.elementType, result))
    }

    private fun registry(project: Project): Registry = synchronized(registryLock) {
        project.getUserData(REGISTRY_KEY) ?: Registry(project).also { project.putUserData(REGISTRY_KEY, it) }
    }

    private class RootResult(val rootType: com.intellij.psi.tree.IElementType, val result: CjParsedFileResult)

    private class Registry(project: Project) : PomModelListener {
        private val model = checkNotNull(project.getService(PomModel::class.java))
        private val aspect = checkNotNull(model.getModelAspect(TreeAspect::class.java))
        private val roots = CollectionFactory.createConcurrentWeakIdentityMap<CharTable, RootResult>()
        private val completedChanges = CollectionFactory.createConcurrentWeakIdentityMap<TreeChangeEvent, Boolean>()

        init {
            model.addModelListener(this, project)
        }

        fun register(table: CharTable, entry: RootResult) {
            val previous = roots.putIfAbsent(table, entry)
            check(previous == null || previous.result === entry.result) {
                "A native parsing table must not be overwritten by another producer."
            }
        }

        override fun isAspectChangeInteresting(changedAspect: PomModelAspect): Boolean = changedAspect === aspect

        /** entries 与嵌套事务完成后才发布；这是 AST adoption，不等同于 Document 已提交。 */
        override fun modelChanged(event: PomModelEvent) {
            val change = event.getChangeSet(aspect) as? TreeChangeEvent ?: return
            // 同一个真实完成changeSet的重投保持幂等；新的body-only事件必须继续撤销完整根依据。
            if (completedChanges.putIfAbsent(change, true) != null) return
            val root = change.rootElement as? FileElement ?: return
            val file = root.psi as? CjFile ?: return
            check(file.node === root) { "A POM result must belong to the actual installed file root." }
            if (root.elementType is ICodeFragmentElementType) {
                // 当前真实FileElement已经采用新表；SDK残留的旧root key不属于当前代次，正式查找回到字段表。
                root.putUserData(CharTable.CHAR_TABLE_KEY, null)
                check(SharedImplUtil.findCharTableByTree(root) === root.charTable)
            }
            val result = if (root.elementType is ICodeFragmentElementType) {
                roots[root.charTable]?.let { entry ->
                    check(entry.rootType === root.elementType)
                    entry.result
                }
            } else {
                root.findChildByType(CjNodeTypes.PACKAGE_DIRECTIVE)?.getCopyableUserData(PARSED_FILE_RESULT_KEY)
            }
            file.invalidatePsiSourceView()
            if (result != null && result !== file.getCopyableUserData(PUBLISHED_FILE_RESULT_KEY)) {
                result.sourceContext.requireInput(root.chars)
                check(root.chars.toString() == result.sourceText)
                file.publishParsedFileResult(result)
            } else {
                file.parsedRoleSnapshot = null
                file.parsedRoleRegion = null
                file.parsedFileFacts = null
                file.parsedLanguageModuleName = null
            }
        }
    }
}
