<?xml version="1.0"?>
<command>
  <copy src="code template/gitignore_file" dest="${rootOut}/.gitignore"/>
  <merge src="code template/project-build-profile.json5.ftl" dest="${projectPath}/build-profile.json5"/>
  <mkdir dest="${rootOut}/libs/arm64-v8a" />

  <#--  cangjie  -->
  <instantiate src="code template/src/common/traditionalCode/cangjie/src/index.cj.ftl"
    dest="${rootOut}/src/main/cangjie/index.cj"/>
  <instantiate src="code template/src/common/traditionalCode/cangjie/cjpm.toml.ftl"
    dest="${rootOut}/src/main/cangjie/cjpm.toml"/>

  <#--  cangjie resource  -->
  <instantiate src="../../common/cj_res/cjpm.toml.ftl" dest="${rootOut}/src/main/cangjie/cj_res/cjpm.toml"/>
  <instantiate src="../../common/cj_res/src/AppR_HAR.cj.ftl" dest="${rootOut}/src/main/cangjie/cj_res/src/AppR.cj"/>
  <instantiate src="../../common/cj_res/src/SysR.cj.ftl" dest="${rootOut}/src/main/cangjie/cj_res/src/SysR.cj"/>

  <#-- hivgor -->
  <copy src="../../common/hvigor/cangjie-build-support-3.1.132.tgz" dest="${projectPath}/hvigor/cangjie-build-support-3.1.132.tgz"/>
  <merge src="code template/project-hvigor-config.json5.ftl" dest="${projectPath}/hvigor/hvigor-config.json5"/>
  <copy src="../../common/hvigorfile.ts" dest="${projectPath}/hvigorfile.ts"/>

<#if apiType == "stageMode">
    <#include  "commands/stageCommand.xml.ftl"/>
</#if>

  <#include "commands/createModuleCommand.xml.ftl" />
</command>
