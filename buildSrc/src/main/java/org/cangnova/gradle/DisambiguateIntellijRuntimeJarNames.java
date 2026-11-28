package org.cangnova.gradle;

import org.gradle.api.Action;
import org.gradle.api.file.FileCopyDetails;

import java.io.File;
import java.io.Serial;
import java.io.Serializable;

/**
 * 在应用分发目录中区分 IntelliJ Platform 与 Fleet 的同名运行时模块。
 *
 * <p>两个模块的 JAR 内容不同，必须同时保留；该 Action 无状态且可序列化，
 * 以便分发任务继续使用 Gradle 配置缓存。</p>
 */
public final class DisambiguateIntellijRuntimeJarNames implements Action<FileCopyDetails>, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    public void execute(FileCopyDetails details) {
        String name = details.getName();
        if (!name.matches("(kernel|rpc)-.+\\.jar")) {
            return;
        }

        File artifact = details.getFile();
        File groupDirectory = artifact.getParentFile().getParentFile().getParentFile().getParentFile();
        String groupId = groupDirectory.getName();
        if (groupId.startsWith("com.jetbrains.intellij.")) {
            String groupSuffix = groupId.substring("com.jetbrains.intellij.".length()).replace('.', '-');
            details.setName(groupSuffix + "-" + name);
        }
    }
}
