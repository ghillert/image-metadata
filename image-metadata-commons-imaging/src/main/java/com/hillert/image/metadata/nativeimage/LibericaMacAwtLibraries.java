package com.hillert.image.metadata.nativeimage;

import java.io.File;
import java.util.function.BooleanSupplier;

import com.oracle.svm.core.annotate.Alias;
import com.oracle.svm.core.annotate.Substitute;
import com.oracle.svm.core.annotate.TargetClass;

/**
 * NIK 25 statically links the macOS toolkit, but its AWT loader uses
 * JVM_IsStaticallyLinked(), which returns false. Resolve that toolkit through
 * Native Image's built-in library loader instead of opening a nonexistent dylib.
 * The matching Maven profile retains/exports JNI_OnLoad_awt_lwawt.
 *
 * @author Gunnar Hillert
 */
@TargetClass(className = "com.oracle.svm.core.jdk.NativeLibraries",
		onlyWith = LibericaMacAwtLibraries.AffectedToolchain.class)
public final class LibericaMacAwtLibraries {

	@Alias
	private native boolean loadLibrary0(File file, boolean builtin);

	@Alias
	public native void loadLibraryRelative(String name);

	@Substitute
	public void loadLibraryAbsolute(File file) {
		if (file.getName().equals("libawt_lwawt.dylib")) {
			loadLibraryRelative("awt_lwawt");
		}
		else if (!loadLibrary0(file, false)) {
			throw new UnsatisfiedLinkError("Can't load library: " + file);
		}
	}

	/** Evaluated by the image builder, never by the running application. */
	public static final class AffectedToolchain implements BooleanSupplier {

		@Override
		public boolean getAsBoolean() {
			return System.getProperty("os.name", "").equals("Mac OS X")
					&& System.getProperty("java.vendor.version", "").startsWith("Liberica-NIK-")
					&& Runtime.version().feature() == 25;
		}

	}

}
