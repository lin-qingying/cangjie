package org.cangnova.cangjie.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * 编译器构建身份。对应 Kotlin compiler.version/config/KotlinCompilerVersion 的资源发布链。
 * CJO 的 producer 版本独立于 schema 档位及语言选项；资源使用仓颉命名空间，避免依赖中的
 * Kotlin compiler.version 被同一类加载器误读。缺失或未生成资源是构建错误，不能伪造版本。
 */
public final class CangJieCompilerVersion {
    public static final String VERSION_FILE_PATH = "/META-INF/cangjie/compiler.version";
    public static final String VERSION = loadCompilerVersion();

    private CangJieCompilerVersion() {}

    public static String getVersion() {
        return VERSION;
    }

    private static String loadCompilerVersion() {
        InputStream stream = CangJieCompilerVersion.class.getResourceAsStream(VERSION_FILE_PATH);
        if (stream == null) {
            throw new IllegalStateException("Missing compiler build identity: " + VERSION_FILE_PATH);
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String version = reader.readLine();
            if (version == null || version.isBlank() || version.equals("@snapshot@")) {
                throw new IllegalStateException("Compiler build identity was not generated");
            }
            return version;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read compiler version from " + VERSION_FILE_PATH, exception);
        }
    }
}
