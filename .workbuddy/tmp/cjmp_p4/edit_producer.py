root = 'D:/code/intellij/cangjie/cfir/cfir-serialization/src/org/cangnova/cangjie/cfir/serialization/cjo/'
s = ''


def load(p):
    global s
    s = open(p, encoding='utf-8', newline='').read()
    crlf = '\r\n' in s
    s = s.replace('\r\n', '\n')
    return crlf


def save(p, crlf):
    open(p, 'w', encoding='utf-8', newline='').write(s.replace('\n', '\r\n') if crlf else s)


def rep(old, new):
    global s
    assert s.count(old) == 1, old
    s = s.replace(old, new)


p = root + 'CfirCjoPackageMetadataProducer.kt'
c = load(p)
rep('''import org.cangnova.cangjie.cfir.session.languageVersionSettings
''', '''import org.cangnova.cangjie.cfir.session.CfirCjmpMode
import org.cangnova.cangjie.cfir.session.cjmpSettings
import org.cangnova.cangjie.cfir.session.languageVersionSettings
''')
rep('''        private val languageVersion = files.first().moduleData.session.languageVersionSettings.languageVersion
''', '''        private val languageVersion = files.first().moduleData.session.languageVersionSettings.languageVersion
        private val cjmpSettings = files.first().moduleData.session.cjmpSettings

        /**
         * 本次写出的是否为 CJMP common part cjo（官方 common part 编译 = CHIR 输出模式，
         * `ASTWriter` 此时写 `Package.options` 与 `FileInfo.feature`）。
         */
        private val writesCjmpCommonPart =
            languageVersion >= LanguageVersion.CANGJIE_1_1_0 && cjmpSettings.mode == CfirCjmpMode.COMMON
''')
rep('''                schemaProfile = if (languageVersion >= LanguageVersion.CANGJIE_1_1_0) {
                    CjoSchemaProfile.OFFICIAL_V1_1_3
                } else {
                    CjoSchemaProfile.OFFICIAL_V1_0_0
                },''', '''                schemaProfile = when {
                    writesCjmpCommonPart -> CjoSchemaProfile.OFFICIAL_CJMP
                    languageVersion >= LanguageVersion.CANGJIE_1_1_0 -> CjoSchemaProfile.OFFICIAL_V1_1_3
                    else -> CjoSchemaProfile.OFFICIAL_V1_0_0
                },
                // 官方 `ASTWriter::SaveOptions`：common part cjo 内嵌 debug 与优化级别，供 specific
                // 编译的加载门比对（`ASTLoader::ValidateOptions`）
                options = if (writesCjmpCommonPart) {
                    CjoModuleOptionMetadata(
                        debug = cjmpSettings.moduleDebug,
                        optLevel = CjmpCommonPartLoadGate.optLevelOrdinal(cjmpSettings.moduleOptLevel),
                    )
                } else {
                    null
                },''')
rep('''            return CjoFileInfoMetadata(fileIndices.getValue(file), begin, end)''',
    '''            // 官方 `SaveFileInfo`：文件带 features 指令时一并写出（仅 CJMP common part 档位可编码）
            val feature = file.featuresDirective
                ?.takeIf { writesCjmpCommonPart }
                ?.let { directive ->
                    CjoFeaturesDirectiveMetadata(directive.featureIds.map { it.split('.') })
                }
            return CjoFileInfoMetadata(fileIndices.getValue(file), begin, end, feature)''')
save(p, c)
print('ok')
