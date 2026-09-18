package org.cangnova.cangjie.cfir.serialization.cjd

import PackageFormat.DeclKind
import org.cangnova.cangjie.annotations.BuiltInAnnotationRegistry
import org.cangnova.cangjie.cfir.declarations.CfirDeclaration
import org.cangnova.cangjie.cfir.serialization.deserialize.CfirDeserializationContext
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap

/** 一代库上下文拥有的 sidecar 服务。只在声明自己的 publication 前追加注解，不改变二进制签名。 */
interface CjdBinaryAnnotationOverlay {
    val snapshot: CjdSidecarIndex?
    val matchDiagnostics: List<CjdBinaryMatchDiagnostic>
    val conversionDiagnostics: List<CjdAnnotationConversionDiagnostic>
    val loadDiagnostic: String?
    fun annotations(declarationIndex: Int, declaration: CfirDeclaration): List<org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall>

    companion object {
        /** 缺失/不可读的可选 sidecar 不阻断库；其余错误（含取消）原样传播。 */
        fun load(context: CfirDeserializationContext, sourcePath: Path): CjdBinaryAnnotationOverlay {
            val path = CjdSidecarLocator.findReadable(sourcePath)
                ?: return BinaryAnnotationOverlay(context, null, null)
            return try {
                BinaryAnnotationOverlay(context, CjdSidecarParser.create().parse(path), null)
            } catch (e: IOException) {
                BinaryAnnotationOverlay(context, null, "$path: ${e.message}")
            }
        }
    }
}

/**
 * [CjdBinaryAnnotationOverlay] 的默认实现。
 *
 * 持有匹配计划与注解转换器；转换期产生的诊断按声明索引聚合，
 * 读取时按索引排序输出，保证诊断顺序稳定可复现。
 */
private class BinaryAnnotationOverlay(
    context: CfirDeserializationContext,
    override val snapshot: CjdSidecarIndex?,
    override val loadDiagnostic: String?,
) : CjdBinaryAnnotationOverlay {
    private val plan = snapshot?.let { CjdBinaryDeclarationMatcher.create(context, it) }
    private val converter = CjdAnnotationConverter.create()
    private val issues = ConcurrentHashMap<Int, List<CjdAnnotationConversionDiagnostic>>()
    override val matchDiagnostics: List<CjdBinaryMatchDiagnostic> = plan?.diagnostics.orEmpty()
    override val conversionDiagnostics: List<CjdAnnotationConversionDiagnostic>
        get() = issues.toSortedMap().values.flatten()
    private val resolution = object : CjdAnnotationResolutionContext {
        override val implicitSystemAnnotations: Set<FqName> = context.implicitSystemAnnotations
        override fun findClassIds(fqName: FqName, organizationName: String?): List<ClassId> {
            // 仅查询原始表；禁止为了注解构造器而重入正在物化的父声明。
            val packageName = fqName.parent().asString()
            val pkg = if (packageName == context.header.fullPkgName) context.pkg
                else context.cjoManager.loadPackage(packageName) ?: return emptyList()
            if (organizationName != null && pkg.moduleName != organizationName) return emptyList()
            return (0 until pkg.allDeclsLength).mapNotNull { index ->
                val decl = pkg.allDecls(index) ?: return@mapNotNull null
                if (decl.isTopLevel && decl.identifier == fqName.shortName().asString() &&
                    decl.kind in setOf(DeclKind.ClassDecl, DeclKind.StructDecl, DeclKind.InterfaceDecl, DeclKind.EnumDecl)) {
                    ClassId(FqName(packageName), Name.identifier(decl.identifier!!))
                } else null
            }
        }
    }

    override fun annotations(declarationIndex: Int, declaration: CfirDeclaration): List<org.cangnova.cangjie.cfir.expressions.CfirAnnotationCall> {
        val syntax = snapshot ?: return emptyList()
        val selection = plan?.selection(declarationIndex) ?: return emptyList()
        if (selection.annotations.isEmpty()) return emptyList()
        val result = converter.convert(selection.annotations, declaration.symbol, syntax.annotationContext, resolution, syntax.sourceId)
        issues[declarationIndex] = result.diagnostics
        return result.annotations
    }
}

/** HarmonyOS 平台的显式预导入策略；普通本机/第三方库不通过短名自动提升为系统注解。 */
fun cjdImplicitSystemAnnotations(sourcePath: Path?): Set<FqName> =
    if (sourcePath?.any { it.toString().contains("_ohos_") } == true)
        BuiltInAnnotationRegistry.systemAndSpecial.mapNotNull { it.classFqName }.toSet()
    else emptySet()
