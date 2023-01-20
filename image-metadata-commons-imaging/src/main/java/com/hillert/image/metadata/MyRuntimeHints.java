package com.hillert.image.metadata;

import com.hillert.image.metadata.controller.validation.ImageFileValidator;
import org.apache.xerces.impl.dv.dtd.DTDDVFactoryImpl;
import org.apache.xerces.parsers.XIncludeAwareParserConfiguration;
import org.springframework.aot.hint.ExecutableMode;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Method;
import java.util.ArrayList;

public class MyRuntimeHints implements RuntimeHintsRegistrar {

	@Override
	public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
		// Register method for reflection
		Method method = ReflectionUtils.findMethod(ArrayList.class, "isEmpty");
		hints.reflection().registerMethod(method, ExecutableMode.INVOKE);

		hints.reflection().registerType(ImageFileValidator.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);

		hints.reflection().registerType(DTDDVFactoryImpl.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
		hints.reflection().registerType(XIncludeAwareParserConfiguration.class, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);

	}
}

