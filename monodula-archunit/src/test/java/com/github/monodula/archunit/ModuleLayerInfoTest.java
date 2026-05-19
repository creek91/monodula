package com.github.monodula.archunit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ModuleLayerInfoTest {

    @Nested
    @DisplayName("fromPackageName")
    class FromPackageName {

        @Test
        @DisplayName("extracts account-core module info")
        void accountCore() {
            ModuleLayerInfo info =
                    ModuleLayerInfo.fromPackageName(
                            "com.yuanbao.growth.master.account.core.account.service.api.impl");

            assertThat(info).isNotNull();
            assertThat(info.base).isEqualTo("com.yuanbao.growth.master");
            assertThat(info.module).isEqualTo("account");
            assertThat(info.layer).isEqualTo("core");
        }

        @Test
        @DisplayName("extracts account-api module info")
        void accountApi() {
            ModuleLayerInfo info =
                    ModuleLayerInfo.fromPackageName(
                            "com.yuanbao.growth.master.account.api.model.req.media");

            assertThat(info).isNotNull();
            assertThat(info.base).isEqualTo("com.yuanbao.growth.master");
            assertThat(info.module).isEqualTo("account");
            assertThat(info.layer).isEqualTo("api");
        }

        @Test
        @DisplayName("extracts common-core module info")
        void commonCore() {
            ModuleLayerInfo info =
                    ModuleLayerInfo.fromPackageName("com.yuanbao.growth.master.common.core.model");

            assertThat(info).isNotNull();
            assertThat(info.base).isEqualTo("com.yuanbao.growth.master");
            assertThat(info.module).isEqualTo("common");
            assertThat(info.layer).isEqualTo("core");
            assertThat(info.isCommonModule()).isTrue();
        }

        @Test
        @DisplayName("extracts common-app module info")
        void commonApp() {
            ModuleLayerInfo info =
                    ModuleLayerInfo.fromPackageName("com.yuanbao.growth.master.common.app.config");

            assertThat(info).isNotNull();
            assertThat(info.module).isEqualTo("common");
            assertThat(info.layer).isEqualTo("app");
            assertThat(info.isCommonModule()).isTrue();
        }

        @Test
        @DisplayName("extracts material-core module info")
        void materialCore() {
            ModuleLayerInfo info =
                    ModuleLayerInfo.fromPackageName(
                            "com.yuanbao.growth.master.material.core.service.impl");

            assertThat(info).isNotNull();
            assertThat(info.base).isEqualTo("com.yuanbao.growth.master");
            assertThat(info.module).isEqualTo("material");
            assertThat(info.layer).isEqualTo("core");
            assertThat(info.isCommonModule()).isFalse();
        }

        @Test
        @DisplayName("extracts third-party core info (different base)")
        void thirdPartyCore() {
            ModuleLayerInfo info = ModuleLayerInfo.fromPackageName("cn.hutool.core.collection");

            assertThat(info).isNotNull();
            assertThat(info.base).isEqualTo("cn");
            assertThat(info.module).isEqualTo("hutool");
            assertThat(info.layer).isEqualTo("core");
        }

        @Test
        @DisplayName("extracts mybatisplus core info (different base)")
        void mybatisplusCore() {
            ModuleLayerInfo info =
                    ModuleLayerInfo.fromPackageName("com.baomidou.mybatisplus.core.metadata");

            assertThat(info).isNotNull();
            assertThat(info.base).isEqualTo("com.baomidou");
            assertThat(info.module).isEqualTo("mybatisplus");
            assertThat(info.layer).isEqualTo("core");
        }

        @Test
        @DisplayName("returns null for package without layer segment")
        void noLayerSegment() {
            ModuleLayerInfo info =
                    ModuleLayerInfo.fromPackageName("com.yuanbao.growth.master.shared.dto");

            assertThat(info).isNull();
        }

        @Test
        @DisplayName("returns null for java.lang package")
        void javaLang() {
            ModuleLayerInfo info = ModuleLayerInfo.fromPackageName("java.lang");

            assertThat(info).isNull();
        }

        @Test
        @DisplayName("returns null for jakarta.servlet package")
        void jakartaServlet() {
            ModuleLayerInfo info = ModuleLayerInfo.fromPackageName("jakarta.servlet.http");

            assertThat(info).isNull();
        }
    }

    @Nested
    @DisplayName("cross-module detection")
    class CrossModule {

        @Test
        @DisplayName("same project, different module = cross-module")
        void sameProjectDifferentModule() {
            ModuleLayerInfo accountCore =
                    ModuleLayerInfo.fromPackageName(
                            "com.yuanbao.growth.master.account.core.service");
            ModuleLayerInfo materialCore =
                    ModuleLayerInfo.fromPackageName(
                            "com.yuanbao.growth.master.material.core.service");

            assertThat(accountCore.isDifferentModule(materialCore)).isTrue();
            assertThat(accountCore.sharesBaseWith(materialCore)).isTrue();
        }

        @Test
        @DisplayName("same project, same module = NOT cross-module")
        void sameProjectSameModule() {
            ModuleLayerInfo accountCore =
                    ModuleLayerInfo.fromPackageName(
                            "com.yuanbao.growth.master.account.core.service.impl");
            ModuleLayerInfo accountCoreRepo =
                    ModuleLayerInfo.fromPackageName(
                            "com.yuanbao.growth.master.account.core.orm.mysql.repository");

            assertThat(accountCore.isDifferentModule(accountCoreRepo)).isFalse();
            assertThat(accountCore.sharesBaseWith(accountCoreRepo)).isTrue();
        }

        @Test
        @DisplayName("different project = NOT cross-module (third-party)")
        void differentProject() {
            ModuleLayerInfo accountCore =
                    ModuleLayerInfo.fromPackageName(
                            "com.yuanbao.growth.master.account.core.service");
            ModuleLayerInfo hutoolCore =
                    ModuleLayerInfo.fromPackageName("cn.hutool.core.collection");

            assertThat(accountCore.sharesBaseWith(hutoolCore)).isFalse();
        }
    }
}
