//--------------------------------------------------------------------------
//THIS SHARED LIBRARY WILL READ LOG FILE FROM WORKSPACE
//AND SETS THE STATUS FOR THE BUILD DEPENDING ON THE STRING FOUND
//AUTOR: kpaidi
//--------------------------------------------------------------------------

def call(estatus) {
    
    def errorHandle = [:]
    estatus.resolveStrategy = Closure.DELEGATE_FIRST
    estatus.delegate = errorHandle
    estatus()

    //following code find a string in the log file and sets the STATUS
    //NOTE: do not change the order it should be success,unstable,failure
    //sets the status to success
    errorHandle.success.each {successString ->
                  if(errorHandle.logFile.contains(successString)){
                      currentBuild.result="SUCCESS"
                      echo "Found \u2705${successString}\u2705 Status is set to SUCCESS"
                  }
    };

    //sets the current status to unstable
    errorHandle.unstable.each {unstableString ->
                  if(errorHandle.logFile.contains(unstableString)){
                      currentBuild.result="UNSTABLE"
                      echo "Found \u274C${unstableString}\u274C Status is set to UNSTABLE"
                      //if error file is present it will print the output
  					  if(errorHandle.printFile) {
  						 println "*********Coverity New Defects output******** \n" + errorHandle.printFile
  					  }
                  }
    };

    //sets the current status to failure
    errorHandle.failure.each {failureString ->
                  if(errorHandle.logFile.contains(failureString)){
                      currentBuild.result="FAILURE"
                      echo "Found \u274C ${failureString}\u274C  Status is set to FAILURE"
  					  if(errorHandle.printFile) {
  						 println "*********Coverity New Defects output******** \n" + errorHandle.printFile
  					  }
                  }
    };
  
    //parses the current console output given a rules file.
    errorHandle.parse.each {rulesFile ->
	              echo "Looking for ${rulesFile}"
	              writeFile file:"$WORKSPACE/parsing_rules", text:libraryResource(rulesFile)
		      echo "Parsing..."
                      step([$class: 'LogParserPublisher', 
                          parsingRulesPath: "$WORKSPACE/parsing_rules", 
                          useProjectRule: false,
                          failBuildOnError: true,
                          unstableOnWarning: true
                      ]);
    };
}
