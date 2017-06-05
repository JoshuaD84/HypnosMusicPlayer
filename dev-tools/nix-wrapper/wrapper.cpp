/*Compile this on a linux machine to create a local nix executable
	g++ -D 'jarfile=hypnos.jar' -m32 -o hypnos wrapper.cpp
	-m32 forces 32 bit mode, which should help compatibility
*/

#include <stdio.h>
#include <cstdlib>
#include <string>
#include <iostream>
#include <sys/types.h>
#include <unistd.h>
#include <limits.h>

using namespace std;

#define xstr(s) str(s)
#define str(s) #s

string getPath() {
   char arg1[20];
   char exepath[PATH_MAX + 1] = {0};

   sprintf( arg1, "/proc/%d/exe", getpid() );
   readlink( arg1, exepath, sizeof(exepath) );
   return string( exepath );
}

int main( int argc, char* argv[] ) {
   printf( "Arg0: %s", argv[0] );

   std::cout << "GetPath: " << getPath();

	string jarFile = xstr ( JAR_FILE );
	string command = "java -jar " + jarFile  + " 2>> ~/.hypnos/hypnos.log >> ~/.hypnos/hypnos.log";
	system ( "mkdir -p ~/.hypnos" );
	int result = system ( command.c_str() );
	result = WEXITSTATUS ( result );

	if ( result == 134 ) {
		//Do nothing, this is a wierd bug that doesn't seem to mean anything
	} else if ( result != 0 ) {
		printf ( "Error %d: ", result );
	}
}

