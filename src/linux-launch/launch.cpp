#include <jni.h> 
#include <dlfcn.h>
#include <string>


typedef jint (*p_JNI_CreateJavaVM)(JavaVM**, void**, JavaVMInitArgs* );

int main( int argc, char** argv ) {
   JavaVM *jvm; 
   JNIEnv *env;
   JavaVMInitArgs vm_args; 
   JavaVMOption* options = new JavaVMOption[10];
   
   int index = 0;
   options[index].optionString = (char *)"-Djava.class.path=./hypnos.jar";
   index++;
   options[index].optionString = (char *)"-Xmx500m";
   index++;
   options[index].optionString = (char *)"-XX:+UseG1GC";
   index++;
   options[index].optionString = (char *)"-XX:+UseStringDeduplication";
   index++;
   options[index].optionString = (char *)"-XX:MinHeapFreeRatio=20";
   index++;
   options[index].optionString = (char *)"-XX:MaxHeapFreeRatio=30";
   index++;
   options[index].optionString = (char *)"--module-path=./jfx";
   index++;
   options[index].optionString = (char *)"--add-modules=javafx.controls,javafx.swing";
   index++;
   options[index].optionString = (char *)"--add-opens=javafx.controls/javafx.scene.control=ALL-UNNAMED";
   index++;
   options[index].optionString = (char *)"-Djdk.gtk.version=2";
   index++;

   vm_args.version = JNI_VERSION_10;
   vm_args.nOptions = index;
   vm_args.options = options;
   vm_args.ignoreUnrecognized = false;

   std::string location = "./jre/lib/server/libjvm.so";
   void *handle = dlopen ( location.c_str(), RTLD_LAZY );

   if ( !handle ) {
      printf ( "Unable to load %s, exiting\n", location.c_str() );
      return 0;
   }

   p_JNI_CreateJavaVM JNI_CreateJavaVM = (p_JNI_CreateJavaVM)dlsym(handle, "JNI_CreateJavaVM");

   jint ret = JNI_CreateJavaVM(&jvm, (void**)&env, &vm_args);
   
   //JNI_CreateJavaVM(&jvm, (void**)&env, &vm_args);
   delete options;

   jmethodID main = NULL;
   jclass cls = NULL;
   
   cls = env->FindClass("net/joshuad/hypnos/Hypnos");
   if(env->ExceptionCheck()) {  
      env->ExceptionDescribe();
      env->ExceptionClear();
   }

   if (cls != NULL) {
      main = env->GetStaticMethodID(cls, "main", "([Ljava/lang/String;)V");
   } else {
      printf("Unable to find the requested class\n");
   }

   if (main != NULL) {
      jclass classString = env->FindClass("java/lang/String");
      jobjectArray argsToJava = env->NewObjectArray( argc-1, classString, NULL );

      for ( int i = 1; i < argc; i++ ) {
         jstring arg = env->NewStringUTF( argv[i] );
         env->SetObjectArrayElement( argsToJava, i-1, arg );
      }

      env->CallStaticVoidMethod( cls, main, argsToJava );

   } else {
      printf("main method not found\n") ;
   }

   jvm->DestroyJavaVM();
   return 0;
}

