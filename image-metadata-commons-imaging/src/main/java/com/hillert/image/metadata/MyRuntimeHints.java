package com.hillert.image.metadata;

import java.lang.reflect.Method;
import java.util.ArrayList;

import com.hillert.image.metadata.controller.validation.ImageFileValidator;
import com.hillert.image.metadata.model.GnssInfo;
import org.apache.xerces.impl.dv.dtd.DTDDVFactoryImpl;
import org.apache.xerces.parsers.XIncludeAwareParserConfiguration;

import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.util.ReflectionUtils;

public class MyRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		// Register method for reflection
		Method method = findRequiredMethod(ArrayList.class, "isEmpty");
		hints.reflection().registerMethod(method, ExecutableMode.INVOKE);

		hints.reflection().registerType(ImageFileValidator.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);

		hints.reflection().registerType(GnssInfo.class, MemberCategory.INVOKE_PUBLIC_METHODS);

		hints.reflection().registerType(DTDDVFactoryImpl.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
		hints.reflection().registerType(XIncludeAwareParserConfiguration.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);

		try {
			Class<?> clazz = Class.forName("com.sun.beans.finder.MethodFinder");
			hints.reflection().registerType(clazz, MemberCategory.DECLARED_CLASSES);
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException(ex);
		}

		Method getDefaultToolkitMethod = findRequiredMethod(java.awt.Toolkit.class, "getDefaultToolkit");
		hints.reflection().registerMethod(getDefaultToolkitMethod, ExecutableMode.INVOKE);

		hints.resources().registerPattern("fonts/*");
		hints.resources().registerPattern("static/*");

		hints.jni().getTypeHint(java.util.HashMap.class);
		hints.jni().getTypeHint(com.hillert.image.metadata.service.support.ImageLoader.class);
		hints.jni().registerType(java.util.HashMap.class, MemberCategory.DECLARED_CLASSES, MemberCategory.INVOKE_PUBLIC_METHODS);
		hints.jni().registerType(java.util.ArrayList.class, MemberCategory.DECLARED_CLASSES, MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_DECLARED_CONSTRUCTORS);
		hints.jni().registerType(java.lang.String.class, MemberCategory.DECLARED_CLASSES, MemberCategory.INVOKE_PUBLIC_METHODS);

	}

	private static Method findRequiredMethod(Class<?> clazz, String name) {
		final Method method = ReflectionUtils.findMethod(clazz, name);
		if (method == null) {
			throw new IllegalStateException(String.format("Unable to find method '%s' on class '%s'.", name, clazz.getSimpleName()));
		}
		return method;
	}
}

