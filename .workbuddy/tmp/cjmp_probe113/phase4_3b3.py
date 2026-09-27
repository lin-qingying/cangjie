import os, time

R = 'D:/code/intellij/cangjie'


def read(p):
    with open(p, 'rb') as f:
        return f.read().decode('utf-8')


def robust_write(path, text):
    data = text.encode('utf-8')
    tmp = path + '.cjmp.tmp'
    last = None
    for _ in range(20):
        try:
            with open(tmp, 'wb') as f:
                f.write(data)
            os.replace(tmp, path)
            return
        except OSError as e:
            last = e
            time.sleep(0.5)
    raise last


def replace_once(text, old, new, what):
    if text.count(old) == 1:
        return text.replace(old, new, 1)
    nl = '\r\n' if '\r\n' in text else '\n'
    old2, new2 = old.replace('\n', nl), new.replace('\n', nl)
    c = text.count(old2)
    assert c == 1, f"{what}: anchor count {c} (lf={text.count(old)})"
    return text.replace(old2, new2, 1)


# =====================================================================
# 7) 生产管线：收集当前编译包的 features
# =====================================================================
p = R + '/compiler/frontend/src/org/cangnova/cangjie/frontend/pipeline/CfirFrontendPipelinePhase.kt'
t = read(p)
t = replace_once(
    t,
    '''        val rootModuleName = Name.identifier(configuration.moduleName ?: "main")''',
    '''        // CJMP features 子集门的 specific 侧输入：官方 `CollectFeaturesFromPackage` 取当前
        // 编译包源文件的 features 指令并集。必须在 session factory context 之前落到配置上，
        // 因为加载 common part cjo 时（依赖侧 provider 构造）已经需要它。
        configuration.cjmpPackageFeatures = collectCjmpPackageFeatures(sources.allSources)
        val rootModuleName = Name.identifier(configuration.moduleName ?: "main")''',
    'pipeline features')
t = replace_once(
    t,
    '''    /**
     * Kotlin FIR writes library metadata after frontend analysis.  CFIR keeps''',
    '''    /**
     * 收集当前编译包的 CJMP features 集合（官方 `File::GetFeatures()` 的包级并集）。
     *
     * 只读取 PSI 源文件的前导 `features { ... }` 指令；LightTree 源文件与非 PSI 来源
     * （宏产物、附加源）不参与——官方 features 只存在于源码文件，宏产物不携带。
     */
    private fun collectCjmpPackageFeatures(sources: List<CjSourceFile>): Set<String> = buildSet {
        for (source in sources) {
            val psiFile = (source as? CjPsiSourceFile)?.psiFile as? CjFile ?: continue
            val directive = psiFile.featuresDirective ?: continue
            addAll(directive.featureIds)
        }
    }

    /**
     * Kotlin FIR writes library metadata after frontend analysis.  CFIR keeps''',
    'pipeline helper')
if 'import org.cangnova.cangjie.config.*' in t and 'org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpPackageFeatures' not in t:
    t = replace_once(
        t,
        '''import org.cangnova.cangjie.config.*''',
        '''import org.cangnova.cangjie.config.*
import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpPackageFeatures''',
        'pipeline import')
robust_write(p, t)
print('7 pipeline features plumbing')

# =====================================================================
# 8) 测试基建：CJMP_FEATURES 指令
# =====================================================================
p = R + '/tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/directives/CfirDiagnosticsDirectives.kt'
t = read(p)
t = replace_once(
    t,
    '''    val CJMP_MODE by enumDirective<CfirCjmpMode>(''',
    '''    /**
     * 注入当前编译包的 features 集合，例如 `// CJMP_FEATURES: Foo,Bar`。
     *
     * 官方 features 来自源文件 `features { ... }` 指令；测试数据里没有该指令时用本指令
     * 提供 features 子集门的 specific 侧输入。
     */
    val CJMP_FEATURES by stringDirective(
        description = "Comma/space separated features of the compiled package for CJMP gates.",
    )

    val CJMP_MODE by enumDirective<CfirCjmpMode>(''',
    'directive')
robust_write(p, t)
print('8a CJMP_FEATURES directive')

p = R + '/tests/test-infrastructure/testFixtures/org/cangnova/cangjie/test/services/EnvironmentConfigurator.kt'
t = read(p)
t = replace_once(
    t,
    '''        configuration.apiLevelSyscapBasePath = testDataAnchor?.path''',
    '''        configuration.apiLevelSyscapBasePath = testDataAnchor?.path
        configuration.cjmpPackageFeatures = module.directives[CfirDiagnosticsDirectives.CJMP_FEATURES]
            .flatMap { raw -> raw.split(',', ' ') }
            .map(String::trim)
            .filter(String::isNotEmpty)
            .toSet()''',
    'facade features')
if 'import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpPackageFeatures' not in t:
    t = replace_once(
        t,
        '''import org.cangnova.cangjie.cfir.entrypoint.configuration.enableInteropCJMapping''',
        '''import org.cangnova.cangjie.cfir.entrypoint.configuration.cjmpPackageFeatures
import org.cangnova.cangjie.cfir.entrypoint.configuration.enableInteropCJMapping''',
        'facade import')
robust_write(p, t)
print('8b test configurator features')

print('phase4_3b-part3 complete')
