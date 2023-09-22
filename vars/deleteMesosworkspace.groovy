//--------------------------------------------------------------------------
//THIS SHARED LIBRARY WILL READ WORKSPACE DIRECTORY OF MESOS FROM A JENKISNJOB
//AND DELETES THE SPECIFIED WORKSPACE DIRECTORIES
//AUTOR: kpaidi
//--------------------------------------------------------------------------

def call(mesosdir) {
    
    def workspacedir = [:]
    mesosdir.resolveStrategy = Closure.DELEGATE_FIRST
    mesosdir.delegate = workspacedir
    mesosdir()

    //following code deletes the directory specified in jenkinsjob
    workspacedir.delworkspace.each {
        
        dir("${it}") {
            // clean workspace
            deleteDir()
        }

    };
}
