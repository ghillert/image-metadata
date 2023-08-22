/*
 * Copyright (c) 2023 Gunnar Hillert.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hillert.image.metadata.controller;

import org.springframework.beans.propertyeditors.StringTrimmerEditor;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * {@link ControllerAdvice} that defines a {@link StringTrimmerEditor} so that form fields
 * that are empty or contain leading and/or trailing whitespace are trimmed automatically.
 *
 * @author Gunnar Hillert
 */
@ControllerAdvice
public class GlobalControllerAdvice {

	@InitBinder
	public void initBinder(WebDataBinder binder) {
		final StringTrimmerEditor stringTrimmer = new StringTrimmerEditor(true);
		binder.registerCustomEditor(String.class, stringTrimmer);
	}

}
