#include <jni.h> 
#include <windows.h>

typedef UINT(CALLBACK* JVMDLLFunction)(JavaVM**, void**, JavaVMInitArgs*);

int main(int argc, char** argv) {

	SetDllDirectoryA("jre\\bin");

	HINSTANCE jvmDLL = LoadLibrary(".\\jre\\bin\\server\\jvm.dll");

	if (!jvmDLL) {
		printf("failed to find jvm.dll at specified location, exiting.\n");
		return 1;
	}

	JVMDLLFunction createJavaVMFunction = (JVMDLLFunction)GetProcAddress(jvmDLL, "JNI_CreateJavaVM");

	if (!createJavaVMFunction) {
		printf("Failed to get pointer to JNI_CreateJavaVM function from jvm.dll, exiting\n");
		return 1;
	}

	JavaVM *jvm;
	JNIEnv *env;
	JavaVMInitArgs vm_args;
	JavaVMOption* options = new JavaVMOption[12];

	int index = 0;
	options[index].optionString = (char *)"-Djava.class.path=./hypnos.jar";
	index++;
	options[index].optionString = (char *)"-Djava.library.path=lib;lib/win/jintellitype;lib/win/swt";
	index++;
	options[index].optionString = (char *)"-Xmx500m";
	index++;
	options[index].optionString = (char *)"-XX:+UseG1GC";
	index++;
	options[index].optionString = (char *)"-XX:+UseStringDeduplication";
	index++;
	options[index].optionString = (char *)"-XX:GCTimeRatio=19";
	index++;
	options[index].optionString = (char *)"-XX:MinHeapFreeRatio=20";
	index++;
	options[index].optionString = (char *)"-XX:MaxHeapFreeRatio=30";
	index++;
	options[index].optionString = (char *)"--module-path=./jfx/lib";
	index++;
	options[index].optionString = (char *)"--add-modules=javafx.controls,javafx.swing";
	index++;
	options[index].optionString = (char *)"--add-opens=javafx.controls/javafx.scene.control=ALL-UNNAMED";
	index++;
	options[index].optionString = (char *)"-Djdk.gtk.version=2";
	index++;

	vm_args.version = JNI_VERSION_10;
	vm_args.nOptions = 12;
	vm_args.options = options;
	vm_args.ignoreUnrecognized = false;

	createJavaVMFunction(&jvm, (void**)&env, &vm_args);

	delete options;

	jmethodID main = NULL;
	jclass cls = NULL;

	cls = env->FindClass("net/joshuad/hypnos/Hypnos");
	if (env->ExceptionCheck()) {
		env->ExceptionDescribe();
		env->ExceptionClear();
		printf("Unable to find net.joshuad.hypnos.Hypnos, exiting.\n");
		return 0;
	}

	if (cls != NULL) {
		main = env->GetStaticMethodID(cls, "main", "([Ljava/lang/String;)V");
	}
	else {
		printf("Unable to find main() in java\n");
		return 0;
	}

	if (main != NULL) {
		jclass classString = env->FindClass("java/lang/String");
		jobjectArray argsToJava = env->NewObjectArray(argc - 1, classString, NULL);
		for (int i = 1; i < argc; i++) {
			printf("Converting: %s", argv[i]);
			jstring arg = env->NewStringUTF(argv[i]);
			env->SetObjectArrayElement(argsToJava, i - 1, arg);
		}

		env->CallStaticVoidMethod(cls, main, argsToJava);

	}
	else {
		printf("main method not found");
	}

	jvm->DestroyJavaVM();
	return 0;
}

int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, LPSTR lpCmdLine, int nCmdShow) {
	return main(__argc, __argv);
}