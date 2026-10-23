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
        return new AlarmMethodVisitor(
                super.visitMethod(access, name, descriptor, signature, exceptions));
    }

    private boolean isBroadcastCreation(int opcode, String owner, String method, String descriptor) {
        return opcode == Opcodes.INVOKESTATIC && "android/app/PendingIntent".equals(owner)
                && "getBroadcast".equals(method) && BROADCAST_DESCRIPTOR.equals(descriptor);
    }

    private boolean isLegacyCancellation(int opcode, String owner, String method, String descriptor) {
        return JOB_CLASS.equals(className) && opcode == Opcodes.INVOKEVIRTUAL
                && "android/app/AlarmManager".equals(owner) && "cancel".equals(method)
                && CANCEL_DESCRIPTOR.equals(descriptor);
    }

    private boolean isPrivateRegistration(int opcode, String owner, String method, String descriptor) {
        return (TELEMETRY_CLASS.equals(className) || LOCATION_CLASS.equals(className))
                && opcode == Opcodes.INVOKEVIRTUAL && "android/content/Context".equals(owner)
                && "registerReceiver".equals(method) && RECEIVER_DESCRIPTOR.equals(descriptor);
    }

    private String broadcastBridge() {
        if (JOB_CLASS.equals(className)) {
            return "getImmutableBroadcast";
        }
        if (LOCATION_CLASS.equals(className)) {
            return "getPrivateMutableBroadcast";
        }
        return "getPrivateImmutableBroadcast";
    }

    private final class AlarmMethodVisitor extends MethodVisitor {
        private AlarmMethodVisitor(MethodVisitor next) {
            super(Opcodes.ASM9, next);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String method,
                String descriptor, boolean isInterface) {
            if (isBroadcastCreation(opcode, owner, method, descriptor)) {
                broadcastCalls++;
                super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER,
                        broadcastBridge(), descriptor, false);
                return;
            }
            if (isLegacyCancellation(opcode, owner, method, descriptor)) {
                cancelCalls++;
                super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "cancelIfPresent",
                        "(Landroid/app/AlarmManager;Landroid/app/PendingIntent;)V", false);
                return;
            }
            if (isPrivateRegistration(opcode, owner, method, descriptor)) {
                receiverCalls++;
                super.visitMethodInsn(Opcodes.INVOKESTATIC, HELPER, "registerPrivateReceiver",
                        "(Landroid/content/Context;Landroid/content/BroadcastReceiver;"
                                + "Landroid/content/IntentFilter;)Landroid/content/Intent;", false);
                return;
            }
            super.visitMethodInsn(opcode, owner, method, descriptor, isInterface);
        }
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
