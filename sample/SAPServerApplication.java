/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  org.springframework.boot.Banner$Mode
 *  org.springframework.boot.autoconfigure.SpringBootApplication
 *  org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration
 *  org.springframework.boot.builder.SpringApplicationBuilder
 *  org.springframework.boot.context.ApplicationPidFileWriter
 *  org.springframework.context.ApplicationListener
 */
package org.talend.sap.impl.server;

import java.util.Properties;
import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.ApplicationPidFileWriter;
import org.springframework.context.ApplicationListener;

@SpringBootApplication(exclude={GsonAutoConfiguration.class})
public class SAPServerApplication {
    public static void main(String[] args) {
        Properties properties = new Properties();
        properties.setProperty("app.name", "tsap-rfc-server");
        properties.setProperty("spring.pid.fail-on-write-error", Boolean.TRUE.toString());
        new SpringApplicationBuilder(new Class[0]).bannerMode(Banner.Mode.OFF).sources(new Class[]{SAPServerApplication.class}).properties(properties).listeners(new ApplicationListener[]{new ApplicationPidFileWriter("tsap-rfc-server.pid")}).run(args);
    }
}
