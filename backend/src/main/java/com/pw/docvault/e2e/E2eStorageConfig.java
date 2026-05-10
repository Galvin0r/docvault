package com.pw.docvault.e2e;

import com.google.cloud.storage.Storage;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URL;

@Profile("e2e")
@Configuration
public class E2eStorageConfig {

    @Bean
    public Storage storage() {
        return (Storage) Proxy.newProxyInstance(
                Storage.class.getClassLoader(),
                new Class<?>[]{Storage.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return switch (method.getName()) {
                            case "toString" -> "E2E Storage Stub";
                            case "hashCode" -> System.identityHashCode(proxy);
                            case "equals" -> proxy == args[0];
                            default -> null;
                        };
                    }
                    if ("signUrl".equals(method.getName())) {
                        return URI.create("https://storage.e2e.invalid/docvault").toURL();
                    }
                    Class<?> returnType = method.getReturnType();
                    if (returnType == boolean.class || returnType == Boolean.class) {
                        return true;
                    }
                    if (returnType == int.class || returnType == Integer.class) {
                        return 0;
                    }
                    if (returnType == long.class || returnType == Long.class) {
                        return 0L;
                    }
                    if (returnType == short.class || returnType == Short.class) {
                        return (short) 0;
                    }
                    if (returnType == byte.class || returnType == Byte.class) {
                        return (byte) 0;
                    }
                    if (returnType == double.class || returnType == Double.class) {
                        return 0D;
                    }
                    if (returnType == float.class || returnType == Float.class) {
                        return 0F;
                    }
                    if (returnType == URL.class) {
                        return URI.create("https://storage.e2e.invalid/docvault").toURL();
                    }
                    return null;
                }
        );
    }
}
