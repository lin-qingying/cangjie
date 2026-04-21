<copy src="ets/code template/src/faMode/hvigorfile.ts" dest="${rootOut}/hvigorfile.ts"/>
<#--  传统开发模式  -->
<#if !superVisualEnable>
<#-- common -->
    <instantiate src="ets/code template/src/faMode/config.json.ftl" dest="${rootOut}/src/main/config.json"/>
    <instantiate src="ets/code template/src/common/traditionalCode/build-profile.json5.ftl" dest="${rootOut}/build-profile.json5"/>
    <#if hasOhPackageJson5 ! false>
        <instantiate src="ets/code template/src/common/traditionalCode/oh-package-fa.json5.ftl" dest="${rootOut}/oh-package.json5"/>
    <#else>
        <instantiate src="ets/code template/src/common/traditionalCode/package.json.ftl" dest="${rootOut}/package.json"/>
    </#if>
    <#if uiSyntax?lower_case == "ets">
    <#--  res  -->
        <copy src="ets/code template/src/common/traditionalCode/ets/res/element/string.json" dest="${rootOut}/src/main/resources/base/element/string.json"/>
        <copy src="ets/code template/src/common/traditionalCode/ets/res/element/string.json" dest="${rootOut}/src/main/resources/en_US/element/string.json"/>
        <copy src="ets/code template/src/common/traditionalCode/ets/res/element/string.json" dest="${rootOut}/src/main/resources/zh_CN/element/string.json"/>
    <#--  src  -->
        <instantiate src="ets/code template/src/common/traditionalCode/ets/index.ets.ftl" dest="${rootOut}/index.ets"/>
        <#if enableNative>
            <copy src="ets/code template/src/common/traditionalCode/cpp/types/libnpmlib/Index.d.ts"
                  dest="${rootOut}/src/main/cpp/types/lib${moduleName?lower_case}/Index.d.ts"/>
            <instantiate src="ets/code template/src/common/traditionalCode/cpp/napi_init.cpp.ftl"
                  dest="${rootOut}/src/main/cpp/napi_init.cpp"/>
            <instantiate src = "ets/code template/src/common/traditionalCode/cpp/CMakeLists.txt.ftl"
                         dest="${rootOut}/src/main/cpp/CMakeLists.txt"/>
            <instantiate src="ets/code template/src/common/traditionalCode/ets/ets cpp/ets/main_page/main_page.ets.ftl"
                  dest="${rootOut}/src/main/ets/components/${pagePackageName}.ets"/>
            <#if  hasOhPackageJson5 ! false>
                <instantiate src="ets/code template/src/common/traditionalCode/cpp/types/libnpmlib/oh-package.json5.ftl"
                             dest="${rootOut}/src/main/cpp/types/lib${moduleName?lower_case}/oh-package.json5"/>
                <merge src="ets/code template/src/common/traditionalCode/cpp-oh-package.json.ftl" dest="${rootOut}/oh-package.json5"/>
            <#else>
                <instantiate src="ets/code template/src/common/traditionalCode/cpp/types/libnpmlib/package.json.ftl"
                             dest="${rootOut}/src/main/cpp/types/lib${moduleName?lower_case}/package.json"/>
                <merge src="ets/code template/src/common/traditionalCode/cpp-package.json.ftl" dest="${rootOut}/package.json"/>
            </#if>
        <#else>
            <instantiate src="ets/code template/src/common/traditionalCode/ets/ets/main_page/main_page.ets.ftl"
                         dest="${rootOut}/src/main/ets/components/${pagePackageName}.ets"/>
        </#if>
        <open file="${rootOut}/src/main/ets/components/${pagePackageName}.ets"/>
    <#elseif uiSyntax?lower_case == "js" || uiSyntax?lower_case == "hml">
    <#--  res  -->
        <copy src="ets/code template/src/common/traditionalCode/js/res/element/string.json" dest="${rootOut}/src/main/resources/base/element/string.json"/>
        <copy src="ets/code template/src/common/traditionalCode/js/res/element/string.json" dest="${rootOut}/src/main/resources/en_US/element/string.json"/>
        <copy src="ets/code template/src/common/traditionalCode/js/res/zh_CN/element/string.json" dest="${rootOut}/src/main/resources/zh_CN/element/string.json"/>
    <#--  src  -->
        <copy src="ets/code template/src/common/traditionalCode/js/index.js" dest="${rootOut}/index.js"/>
        <copy src="ets/code template/src/common/traditionalCode/js/js/common" dest="${rootOut}/src/main/js/common"/>
        <copy src="ets/code template/src/common/traditionalCode/js/js/i18n" dest="${rootOut}/src/main/js/i18n"/>
        <#if enableNative>
            <copy src="ets/code template/src/common/traditionalCode/js/js cpp/js/components" dest="${rootOut}/src/main/js/components"/>
            <instantiate src="ets/code template/src/common/traditionalCode/js/js cpp/js/index.js.ftl"
                         dest="${rootOut}/src/main/js/components/index/index.js"/>
            <copy src="ets/code template/src/common/traditionalCode/cpp/types/libnpmlib/index.d.ts"
                  dest="${rootOut}/src/main/cpp/types/lib${moduleName?lower_case}/index.d.ts"/>
            <instantiate src="ets/code template/src/common/traditionalCode/cpp/napi_init.cpp.ftl"
                  dest="${rootOut}/src/main/cpp/napi_init.cpp"/>
            <instantiate src = "ets/code template/src/common/traditionalCode/cpp/CMakeLists.txt.ftl"
                         dest="${rootOut}/src/main/cpp/CMakeLists.txt"/>
            <#if hasOhPackageJson5 ! false>
                <instantiate src="ets/code template/src/common/traditionalCode/cpp/types/libnpmlib/oh-package.json5.ftl"
                             dest="${rootOut}/src/main/cpp/types/lib${moduleName?lower_case}/oh-package.json5"/>
                <merge src="ets/code template/src/common/traditionalCode/cpp-oh-package.json.ftl" dest="${rootOut}/oh-package.json5"/>
            <#else>
                <instantiate src="ets/code template/src/common/traditionalCode/cpp/types/libnpmlib/package.json.ftl"
                             dest="${rootOut}/src/main/cpp/types/lib${moduleName?lower_case}/package.json"/>
                <merge src="ets/code template/src/common/traditionalCode/cpp-package.json.ftl" dest="${rootOut}/package.json"/>
            </#if>
        <#else>
            <copy src="ets/code template/src/common/traditionalCode/js/js/components" dest="${rootOut}/src/main/js/components"/>
        </#if>
        <open file="${rootOut}/src/main/js/components/index/index.js"/>
    </#if>
<#else>
<#--  低代码开发模式  -->
    <#if uiSyntax?lower_case == "ets">

    <#elseif uiSyntax?lower_case == "js" || uiSyntax?lower_case == "hml">

    </#if>
</#if>