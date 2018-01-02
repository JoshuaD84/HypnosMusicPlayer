package net.joshuad.hypnos.workbench.jni;

public class HelloJNI {
	   static {
	      //; // Load native library at runtime
	                                   // hello.dll (Windows) or libhello.so (Unixes)
	   }
	 
	   // Declare a native method sayHello() that receives nothing and returns void
	   private native void sayHello();
	 
	   // Test Driver
	   public static void main(String[] args) {
		   System.loadLibrary("hello ");
	      //new HelloJNI().sayHello();  // invoke the native method
		   System.out.println ( "hi josh" );
	   }
	}