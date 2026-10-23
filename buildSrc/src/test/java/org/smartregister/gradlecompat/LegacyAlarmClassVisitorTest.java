package org.smartregister.gradlecompat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class LegacyAlarmClassVisitorTest {
    @Test
    public void realAndroidJobFixtureRedirectsCreationAndBothCancellationsOnly() throws Exception {
        byte[] original = fixture("legacy.fixture.androidJob", LegacyAlarmClassVisitor.JOB_CLASS);
        List<String> expected = calls(original);
        List<String> adapted = calls(transform(original));

        replace(expected, Opcodes.INVOKESTATIC, "android/app/PendingIntent", "getBroadcast",
                LegacyAlarmClassVisitor.BROADCAST_DESCRIPTOR, "getImmutableBroadcast",
                LegacyAlarmClassVisitor.BROADCAST_DESCRIPTOR, 1);
        replace(expected, Opcodes.INVOKEVIRTUAL, "android/app/AlarmManager", "cancel",
                "(Landroid/app/PendingIntent;)V", "cancelIfPresent",
                "(Landroid/app/AlarmManager;Landroid/app/PendingIntent;)V", 2);
        assertEquals("Every other upstream invocation stays unchanged", expected, adapted);
    }

    @Test
    public void realTelemetryFixtureRedirectsCreationAndRegistrationOnly() throws Exception {
        byte[] original = fixture("legacy.fixture.mapboxTelemetry", LegacyAlarmClassVisitor.TELEMETRY_CLASS);
        List<String> expected = calls(original);
        List<String> adapted = calls(transform(original));

        replace(expected, Opcodes.INVOKESTATIC, "android/app/PendingIntent", "getBroadcast",
                LegacyAlarmClassVisitor.BROADCAST_DESCRIPTOR, "getPrivateImmutableBroadcast",
                LegacyAlarmClassVisitor.BROADCAST_DESCRIPTOR, 1);
        replace(expected, Opcodes.INVOKEVIRTUAL, "android/content/Context", "registerReceiver",
                "(Landroid/content/BroadcastReceiver;Landroid/content/IntentFilter;)Landroid/content/Intent;",
                "registerPrivateReceiver",
                "(Landroid/content/Context;Landroid/content/BroadcastReceiver;"
                        + "Landroid/content/IntentFilter;)Landroid/content/Intent;", 1);
        assertEquals("Scheduling, receiver lifecycle and every other call stay unchanged", expected, adapted);
    }

    @Test
    public void realLocationFixtureKeepsMutableResultDeliveryAndPrivateReceiver() throws Exception {
        byte[] original = fixture("legacy.fixture.mapboxTelemetry", LegacyAlarmClassVisitor.LOCATION_CLASS);
        List<String> expected = calls(original);
        List<String> adapted = calls(transform(original));

        replace(expected, Opcodes.INVOKESTATIC, "android/app/PendingIntent", "getBroadcast",
                LegacyAlarmClassVisitor.BROADCAST_DESCRIPTOR, "getPrivateMutableBroadcast",
                LegacyAlarmClassVisitor.BROADCAST_DESCRIPTOR, 1);
        replace(expected, Opcodes.INVOKEVIRTUAL, "android/content/Context", "registerReceiver",
                "(Landroid/content/BroadcastReceiver;Landroid/content/IntentFilter;)Landroid/content/Intent;",
                "registerPrivateReceiver",
                "(Landroid/content/Context;Landroid/content/BroadcastReceiver;"
                        + "Landroid/content/IntentFilter;)Landroid/content/Intent;", 1);
        assertEquals("Location intervals, permissions, callbacks and unregister calls stay unchanged",
                expected, adapted);
    }

    @Test
    public void dependencyShapeChangeFailsInsteadOfLeavingAnUnpatchedCall() throws Exception {
        byte[] original = fixture("legacy.fixture.androidJob", LegacyAlarmClassVisitor.JOB_CLASS);
        ClassWriter changed = new ClassWriter(0);
        new ClassReader(original).accept(new ClassVisitor(Opcodes.ASM9, changed) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9,
                        super.visitMethod(access, name, descriptor, signature, exceptions)) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String method,
                            String descriptor, boolean isInterface) {
                        super.visitMethodInsn(opcode, owner,
                                "getBroadcast".equals(method) ? "differentCreationApi" : method,
                                descriptor, isInterface);
                    }
                };
            }
        }, 0);
        try {
            transform(changed.toByteArray());
            fail("A changed upstream class must require explicit adapter review");
        } catch (IllegalStateException expected) {
            assertTrue(expected.getMessage().contains("Legacy alarm dependency changed"));
        }
    }

    @Test
    public void targetsExactlyTheThreePinnedDependencyClasses() {
        assertTrue(LegacyAlarmClassVisitor.isSupportedClass("com.evernote.android.job.v14.JobProxy14"));
        assertTrue(LegacyAlarmClassVisitor.isSupportedClass("com.mapbox.android.telemetry.AlarmSchedulerFlusher"));
        assertTrue(LegacyAlarmClassVisitor.isSupportedClass(
                "com.mapbox.android.telemetry.location.LocationEngineControllerImpl"));
        assertFalse(LegacyAlarmClassVisitor.isSupportedClass("com.evernote.android.job.v19.JobProxy19"));
        assertFalse(LegacyAlarmClassVisitor.isSupportedClass("com.mapbox.android.telemetry.MapboxTelemetry"));
        assertFalse(LegacyAlarmClassVisitor.isSupportedClass("org.smartregister.chw.application.ChwApplication"));
    }

    private static byte[] transform(byte[] bytes) {
        ClassWriter writer = new ClassWriter(0);
        new ClassReader(bytes).accept(new LegacyAlarmClassVisitor(writer), 0);
        return writer.toByteArray();
    }

    private static List<String> calls(byte[] bytes) {
        List<String> calls = new ArrayList<>();
        new ClassReader(bytes).accept(new ClassVisitor(Opcodes.ASM9) {
            @Override
            public MethodVisitor visitMethod(int access, String name, String descriptor,
                    String signature, String[] exceptions) {
                return new MethodVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitMethodInsn(int opcode, String owner, String method,
                            String descriptor, boolean isInterface) {
                        calls.add(call(opcode, owner, method, descriptor));
                    }
                };
            }
        }, 0);
        return calls;
    }

    private static void replace(List<String> calls, int opcode, String owner, String method,
            String descriptor, String replacement, String replacementDescriptor, int expectedCount) {
        String original = call(opcode, owner, method, descriptor);
        int count = 0;
        for (int i = 0; i < calls.size(); i++) {
            if (original.equals(calls.get(i))) {
                calls.set(i, call(Opcodes.INVOKESTATIC, LegacyAlarmClassVisitor.HELPER,
                        replacement, replacementDescriptor));
                count++;
            }
        }
        assertEquals("Pinned upstream call-site count", expectedCount, count);
    }

    private static String call(int opcode, String owner, String method, String descriptor) {
        return opcode + " " + owner + "." + method + descriptor;
    }

    private static byte[] fixture(String property, String className) throws Exception {
        String path = System.getProperty(property);
        assertTrue("Missing pinned AAR fixture property: " + property, path != null);
        try (ZipFile aar = new ZipFile(path);
                InputStream classes = aar.getInputStream(aar.getEntry("classes.jar"));
                ZipInputStream jar = new ZipInputStream(classes)) {
            ZipEntry entry;
            while ((entry = jar.getNextEntry()) != null) {
                if (entry.getName().equals(className + ".class")) {
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    byte[] buffer = new byte[4096];
                    int count;
                    while ((count = jar.read(buffer)) != -1) bytes.write(buffer, 0, count);
                    return bytes.toByteArray();
                }
            }
        }
        throw new AssertionError("Pinned fixture lacks " + className);
    }
}
