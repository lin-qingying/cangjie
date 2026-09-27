root = 'D:/code/intellij/cangjie/'
p = root + 'cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/declaration/CfirCommonSpecificChecker.kt'
s = open(p, encoding='utf-8', newline='').read()
crlf = '\r\n' in s
s = s.replace('\r\n', '\n')
add = open(root + '.workbuddy/tmp/cjmp_p4/checker_append.kt.txt', encoding='utf-8').read()


def rep(old, new):
    global s
    assert s.count(old) == 1, old
    s = s.replace(old, new)


rep('''        // 只对 specific 声明执行匹配检查
        if (declaration.status.isSpecific) {
            checkSpecificMatchesCommon(declaration)
            checkSpecificExtraConstraints(declaration)
        }''', '''        // 配对结论（NOT_MATCHED / 种类 / 修饰符 / 超类型 / 穷尽性）统一由 CfirCjmpMatchingChecker
        // 与 CfirCjmpCommonSideChecker 消费配对存储报告；此处只保留 specific 额外约束。
        if (declaration.status.isSpecific) {
            checkSpecificExtraConstraints(declaration)
        }''')
imports = '''import org.cangnova.cangjie.cfir.session.symbolProvider
import org.cangnova.cangjie.cfir.session.dependenciesSymbolProvider
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.cjmpHasCommonDefault
import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.CfirCjmpMappingStorage
import org.cangnova.cangjie.cfir.session.CjmpMismatchKind
import org.cangnova.cangjie.cfir.declarations.CfirCallableDeclaration
import org.cangnova.cangjie.cfir.declarations.CfirConstructor
import org.cangnova.cangjie.cfir.declarations.CfirDeclarationStatus
import org.cangnova.cangjie.cfir.declarations.CfirExtend
import org.cangnova.cangjie.cfir.declarations.CfirFile
import org.cangnova.cangjie.cfir.declarations.CfirFunction
import org.cangnova.cangjie.cfir.declarations.CfirVariable
import org.cangnova.cangjie.cfir.types.ConeCangJieType
import org.cangnova.cangjie.name.ClassId
'''
rep('import org.cangnova.cangjie.cfir.session.symbolProvider\n', imports)
s = s.rstrip('\n') + '\n' + add
open(p, 'w', encoding='utf-8', newline='').write(s.replace('\n', '\r\n') if crlf else s)

# storage: markCommonSideChecked
p = root + 'cfir/cfir-tree/src/org/cangnova/cangjie/cfir/session/CfirCjmpMappingStorage.kt'
s = open(p, encoding='utf-8', newline='').read()
crlf = '\r\n' in s
s = s.replace('\r\n', '\n')
rep('''    /** 记录参数级失败（官方在参数上报告诊断后匹配失败）。 */''', '''    /** 已完成 common 方向检查的包（每包只检查一次，多文件包去重）。 */
    private val commonSideCheckedPackages = HashSet<String>()

    /** 标记包的 common 方向检查；首次标记返回 true。 */
    open fun markCommonSideChecked(packageFqName: String): Boolean = commonSideCheckedPackages.add(packageFqName)

    /** 记录参数级失败（官方在参数上报告诊断后匹配失败）。 */''')
rep('''        parameterMismatches.clear()
    }''', '''        parameterMismatches.clear()
        commonSideCheckedPackages.clear()
    }''')
open(p, 'w', encoding='utf-8', newline='').write(s.replace('\n', '\r\n') if crlf else s)

# registration
p = root + 'cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/checkers/CommonDeclarationCheckers.kt'
s = open(p, encoding='utf-8', newline='').read()
crlf = '\r\n' in s
s = s.replace('\r\n', '\n')
rep('            CfirCjmpParseRulesChecker,\n', '            CfirCjmpParseRulesChecker,\n            CfirCjmpMatchingChecker,\n')
rep('            CfirCjmpFilePartChecker,\n', '            CfirCjmpFilePartChecker,\n            CfirCjmpCommonSideChecker,\n')
open(p, 'w', encoding='utf-8', newline='').write(s.replace('\n', '\r\n') if crlf else s)

# diagnostic param types
p = root + 'cfir/checkers/checkers-component-generator/src/org/cangnova/cangjie/cfir/checkers/generator/diagnostics/CfirDiagnosticsList.kt'
s = open(p, encoding='utf-8', newline='').read()
crlf = '\r\n' in s
s = s.replace('\r\n', '\n')
rep('''        val SPECIFIC_VAR_NOT_MATCH_LET by error<PsiElement> {
            parameter<Name>("specificName")
            parameter<Name>("commonName")
        }''', '''        val SPECIFIC_VAR_NOT_MATCH_LET by error<PsiElement> {
            // 官方参数（CheckCJMP.cpp MatchCJMPVar）：specific 与 common 的 "var"/"let"
            parameter<String>("specificKind")
            parameter<String>("commonKind")
        }''')
rep('''        val SPECIFIC_MEMBER_MUST_HAVE_IMPLEMENTATION by error<PsiElement> {
            parameter<String>("memberKind")
            parameter<String>("containerKind")
        }''', '''        val SPECIFIC_MEMBER_MUST_HAVE_IMPLEMENTATION by error<PsiElement> {
            // 官方参数（CheckCJMP.cpp TrySetSpecificImpl）：成员名与外层声明名
            parameter<String>("memberName")
            parameter<String>("containerName")
        }''')
open(p, 'w', encoding='utf-8', newline='').write(s.replace('\n', '\r\n') if crlf else s)

p = root + 'cfir/checkers/src/org/cangnova/cangjie/cfir/analysis/diagnostics/CfirErrorsDefaultMessages.kt'
s = open(p, encoding='utf-8', newline='').read()
crlf = '\r\n' in s
s = s.replace('\r\n', '\n')
rep('''map.put(CfirErrors.SPECIFIC_VAR_NOT_MATCH_LET, "'specific' ''{0}'' can not match 'common' ''{1}''", RENDER_NAME, RENDER_NAME)''',
    '''map.put(CfirErrors.SPECIFIC_VAR_NOT_MATCH_LET, "'specific' ''{0}'' can not match 'common' ''{1}''", RENDER_STRING, RENDER_STRING)''')
open(p, 'w', encoding='utf-8', newline='').write(s.replace('\n', '\r\n') if crlf else s)
print('ok')
