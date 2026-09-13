package br.com.engine.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.engine.main.RuntimeProfile;

class RuntimeProfileTest
{
    private String profile;
    private String imageCode;

    @BeforeEach void captureProperties()
    {
        profile = System.getProperty(RuntimeProfile.PROFILE_PROPERTY);
        imageCode = System.getProperty("org.graalvm.nativeimage.imagecode");
    }

    @AfterEach void restoreProperties()
    {
        restore(RuntimeProfile.PROFILE_PROPERTY, profile);
        restore("org.graalvm.nativeimage.imagecode", imageCode);
    }

    @Test void selectsJvmAndNativeProfilesBeforePlatformStartup()
    {
        System.clearProperty(RuntimeProfile.PROFILE_PROPERTY);
        System.clearProperty("org.graalvm.nativeimage.imagecode");
        assertEquals(RuntimeProfile.JVM_FFM, RuntimeProfile.current());
        System.setProperty("org.graalvm.nativeimage.imagecode", "runtime");
        assertEquals(RuntimeProfile.NATIVE_JNI, RuntimeProfile.current());
        System.setProperty(RuntimeProfile.PROFILE_PROPERTY, "jvm-ffm");
        assertEquals(RuntimeProfile.JVM_FFM, RuntimeProfile.current());
        System.setProperty(RuntimeProfile.PROFILE_PROPERTY, "native_jni");
        assertEquals(RuntimeProfile.NATIVE_JNI, RuntimeProfile.current());
    }

    @Test void rejectsAnUnknownProfile()
    {
        System.setProperty(RuntimeProfile.PROFILE_PROPERTY, "hybrid");
        assertThrows(IllegalArgumentException.class, RuntimeProfile::current);
    }

    private static void restore(String key, String value)
    {
        if (value == null) System.clearProperty(key); else System.setProperty(key, value);
    }
}
