package org.cangnova.cangjie.cfir.serialization.cjo

import java.io.File

/**
 * CJO 资源路径的基础规范化：目录仍为目录，具体文件仍为该文件，不扩大 owner 范围。
 * VFS/项目 root 的发现与 ModuleDataProvider 装配留在 LL/Analysis producer；这里不推断模块或 SDK。
 */
fun Iterable<File>.distinctCjoSearchRoots(): List<File> =
    map { it.absoluteFile.normalize() }.distinctBy(File::getPath)
