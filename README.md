# LibDiff
A tool for computing and maintaining version differences(diffs) in Android libraries. The diff is a version-specific "signature" which describes the class files that are unique(either new or modified) to the version of a library, as well as class files that have likely been copied(ie not changed in one or more versions) or deleted.



## DEPENDENCIES
- Java 1.7+


## GET THE TOOL
Simply clone this repository to your local machine.


## TRY IT OUT
The easiest way to use this tool is by creating a runnable Jar. To do so:

1. Import this project into Eclipse:

	```Eclipse
	File > Import > General > Existing Projects into Workspace 

	Select the folder where the tool is cloned to

	Click "Finish" to import this project into Eclipse
	```

2. Create a runnable JAR file:

	```Eclipse
	File > Export > Java > Runnable JAR file

	Choose a destination and name for the JAR

	Click "Finish" to create the runnable JAR
	```

## USAGE
This tool works on a directory which is a collection of libraries - the "Libraries Whitelist". 
This Whitelist is a collection of library JAR's or AAR's that must be manually downloaded (for example from the [maven repository](http://mvnrepository.com/)) and copied to the directory.

When new libraries are added to the directory, the tool will try to compute diffs for the library based on all other versions of it.
The directory structure of this Libraries Whitelist directory MUST look like the following:

![Whitelist Directory Structure](https://github.com/zchi88/LibDiff/blob/master/LibDiff%20Structure.png?raw=true "Whitelist Directory Structure")

Note that the tool currently only accepts AAR's and JAR's of COMPILED library code (e.g. ".class" files, not ".java" source files).

To use the tool to maintain the diffs for the Libraries Whitelist:

1. Open up a command line client
2. Change directory to `PATH/TO/LibDiffTool.jar`
3. Run the tool by typing the following command

	```console
	java -jar LibDiffTool.jar PATH/TO/WHITELIST_LIBRARIES
	```

4. That's it! The tool will perform a startup scan upon starting up to make sure that diffs for libraries that are already present
have already been computed. Afterwards, it will continue to run and look for new libraries being added to the whitelist.

### NOTES
The ```diff.txt``` file for any JAR can be found in the directory where the JAR's class files are extracted to, named after the JAR. This directory exists in the same location as the JAR file.

For example, if we have a library ```"Library1"``` at ```PATH/TO/WHITELIST_LIBRARIES/Library1```, version1.jar of this library will extract to ```PATH/TO/WHITELIST_LIBRARIES/Library1/version1```, and the diff file for this version will be created at ```PATH/TO/WHITELIST_LIBRARIES/Library1/version1/diff.txt```. This is illustrated in the diagram below:

![Extracted Resources Structure](https://raw.githubusercontent.com/zchi88/LibDiff/master/Extracted%20Resources%20Structure.png "Extracted Resources Structure")

Currently, the JAR files must not be deleted for the diff computation to work because version sequencing information is extracted from the JAR's release dates. Without knowledge of this release sequence, the tool cannot compute version-to-version differences.