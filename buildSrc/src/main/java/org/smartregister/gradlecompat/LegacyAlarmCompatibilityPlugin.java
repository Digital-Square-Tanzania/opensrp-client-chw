package org.smartregister.gradlecompat;

import com.android.build.api.instrumentation.AsmClassVisitorFactory;
import com.android.build.api.instrumentation.ClassContext;
import com.android.build.api.instrumentation.ClassData;
import com.android.build.api.instrumentation.InstrumentationParameters;
import com.android.build.api.instrumentation.InstrumentationScope;
import com.android.build.api.variant.ApplicationAndroidComponentsExtension;
import com.android.build.api.variant.ApplicationVariant;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.objectweb.asm.ClassVisitor;

import kotlin.Unit;

/** Narrow compatibility adapter for the two pinned legacy alarm dependencies. */
public final class LegacyAlarmCompatibilityPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        project.getPluginManager().withPlugin("com.android.application", ignored -> {
            ApplicationAndroidComponentsExtension components = project.getExtensions()
                    .getByType(ApplicationAndroidComponentsExtension.class);
            components.onVariants(components.selector().all(), (Action<ApplicationVariant>) variant ->
                    variant.getInstrumentation().transformClassesWith(
                            AlarmVisitorFactory.class, InstrumentationScope.ALL,
                            parameters -> Unit.INSTANCE));
        });
    }

    public abstract static class AlarmVisitorFactory
            implements AsmClassVisitorFactory<InstrumentationParameters.None> {
        @Override
        public boolean isInstrumentable(ClassData classData) {
            return LegacyAlarmClassVisitor.isSupportedClass(classData.getClassName());
        }

        @Override
        public ClassVisitor createClassVisitor(ClassContext classContext, ClassVisitor next) {
            return new LegacyAlarmClassVisitor(next);
        }
    }
}
