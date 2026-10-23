package org.smartregister.gradlecompat;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * Redirects only known platform calls, preserving their stack effects and upstream control flow.
 * Unexpected upstream changes fail the build instead of silently leaving a partial adapter.
 */
final class LegacyAlarmClassVisitor extends ClassVisitor {
    static final String JOB_CLASS = "com/evernote/android/job/v14/JobProxy14";
    static final String TELEMETRY_CLASS = "com/mapbox/android/telemetry/AlarmSchedulerFlusher";
    static final String LOCATION_CLASS = "com/mapbox/android/telemetry/location/LocationEngineControllerImpl";
    static final String HELPER = "org/smartregister/chw/util/LegacyAlarmCompat";
    static final String BROADCAST_DESCRIPTOR =
            "(Landroid/content/Context;ILandroid/content/Intent;I)Landroid/app/PendingIntent;";
    private static final String CANCEL_DESCRIPTOR = "(Landroid/app/PendingIntent;)V";
    private static final String RECEIVER_DESCRIPTOR =
            "(Landroid/content/BroadcastReceiver;Landroid/content/IntentFilter;)Landroid/content/Intent;";

    private String className;
    private int broadcastCalls;
    private int cancelCalls;
    private int receiverCalls;

    LegacyAlarmClassVisitor(ClassVisitor next) {
        super(Opcodes.ASM9, next);
    }

    static boolean isSupportedClass(String name) {
        String internalName = name.replace('.', '/');
        return JOB_CLASS.equals(internalName) || TELEMETRY_CLASS.equals(internalName)
                || LOCATION_CLASS.equals(internalName);
    }

    @Override
    public void visit(int version, int access, String name, String signature,
            String superName, String[] interfaces) {
        className = name;
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor,
            String signature, String[] exceptions) {
        return new MethodVisitor(Opcodes.ASM9,
                super.visitMethod(access, name, descriptor, signature, exceptions)) {
            @Override
            public void visitMethodInsn(int opcode, String owner, String method,
                    String methodDescriptor, boolean isInterface) {
                if (opcode == Opcodes.INVOKESTATIC && "android/app/PendingIntent".equals(owner)
                        && "getBroadcast".equals(method)
                        && BROADCAST_DESCRIPTOR.equals(methodDescriptor)) {
                    broadcastCalls++;
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER,
                            JOB_CLASS.equals(className) ? "getImmutableBroadcast"
                                    : LOCATION_CLASS.equals(className)
                                            ? "getPrivateMutableBroadcast" : "getPrivateImmutableBroadcast",
                            methodDescriptor, false);
                    return;
                }
                if (JOB_CLASS.equals(className) && opcode == Opcodes.INVOKEVIRTUAL
                        && "android/app/AlarmManager".equals(owner) && "cancel".equals(method)
                        && CANCEL_DESCRIPTOR.equals(methodDescriptor)) {
                    cancelCalls++;
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "cancelIfPresent",
                            "(Landroid/app/AlarmManager;Landroid/app/PendingIntent;)V", false);
                    return;
                }
                if ((TELEMETRY_CLASS.equals(className) || LOCATION_CLASS.equals(className))
                        && opcode == Opcodes.INVOKEVIRTUAL
                        && "android/content/Context".equals(owner)
                        && "registerReceiver".equals(method)
                        && RECEIVER_DESCRIPTOR.equals(methodDescriptor)) {
                    receiverCalls++;
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "registerPrivateReceiver",
                            "(Landroid/content/Context;Landroid/content/BroadcastReceiver;"
                                    + "Landroid/content/IntentFilter;)Landroid/content/Intent;", false);
                    return;
                }
                super.visitMethodInsn(opcode, owner, method, methodDescriptor, isInterface);
            }
        };
    }

    @Override
    public void visitEnd() {
        boolean job = JOB_CLASS.equals(className);
        boolean telemetry = TELEMETRY_CLASS.equals(className);
        boolean location = LOCATION_CLASS.equals(className);
        if ((!job && !telemetry && !location) || broadcastCalls != 1
                || cancelCalls != (job ? 2 : 0) || receiverCalls != (job ? 0 : 1)) {
            throw new IllegalStateException("Legacy alarm dependency changed: " + className
                    + ". Review or remove the compatibility adapter (see docs/legacy-alarm-compatibility.md).");
        }
        super.visitEnd();
    }
}
