//--------------------------------------------------------------------------
//THIS SHARED LIBRARY WILL KICK OFF A JENKINS JOB ON A REMOTE SERVER
//AUTHOR: lfenu
//REQUIRES MASK PASSWORDS BUILD PLUGIN
//--------------------------------------------------------------------------

def call(input) {
    def remote = [:]
    input.resolveStrategy = Closure.DELEGATE_FIRST
    input.delegate = remote
    input()

    if (config.parameters != null){
        build("${config.jenkins}", "${config.jobName}", "${config.jobToken}", "{config.auth}", config.parameters)
    } else {
        build("${config.jenkins}", "${config.jobName}", "${config.jobToken}", "{config.auth}")
    }
}

//Trigger a remote build with parameters and sends an email on failure
//recipient - Who to send an email to if the job fails
//jenkins - the URL to the remote Jenkins server
//jobName - The name of the job to trigger (use the full path)
//jobToken - the Token required to trigger the build remotely.
//parameters - The parameters to pass into the job. Pass in as an array of strings.
//auth - the authentication id to use for the call.
def build(recipient, jenkins, jobName, jobToken, auth, parameters){
    try {
        //Construct the basic URL
        def upstreamName = "${env.JOB_NAME}".replace(" ", "_")
        def remoteURL = "${jenkins}/job/${jobName}"
	    
        //Add in the parameters
        def parameterString = ""
        for (param in parameters){
            parameterString = "${parameterString}&$param"
        }
        remoteURL = "$remoteURL/buildWithParameters?$parameterString&token=${jobToken}&cause=Started+by+${upstreamName}"
	
	//Mask the Auth and Crumb in the console log.
	def response
	wrap([$class: 'MaskPasswordsBuildWrapper', varPasswordPairs: [[password: "${auth}", var: 'AUTH']]]) {
            //Get the crumb from jenkins
            def crumbURL = "$jenkins/crumbIssuer/api/json"
            def crumbResponse = httpRequest url: "$crumbURL", authentication: "${auth}", ignoreSslErrors: true
            def crumb = crumbResponse.content.tokenize(",")
            def crumbName = crumb[2].tokenize('"')[2]
            def crumbValue = crumb[1].tokenize('"')[2]

            //Send out the request
	    wrap([$class: 'MaskPasswordsBuildWrapper', varPasswordPairs: [[password: "${crumbValue}", var: 'CRUMB']]]) {
                response = httpRequest httpMode: 'POST', url:"$remoteURL", authentication: "${auth}", customHeaders: [[name: "$crumbName", value: "$crumbValue"]], ignoreSslErrors: true
	    }
        }
 
        //Receive the response
        echo "Received response " + response.status
        if (response.status != 201){
            error "Check remote Jenkins instance connectivity and availability of parameter build/build trigger button on job"
        }else{
            echo "$jobName kicked off successfully."
        }
    } catch (e){
        notifyFailed(recipient, jenkins, jobName)
        error "${e}"
    }
}

//For doing a build on a non-parameterized job sending an email on failure
def build(recipient, jenkins, jobName, jobToken, auth){
    try{
        //Construct the basic URL
        def upstreamName = "${env.JOB_NAME}".replace(" ", "_")
        def remoteURL = "${jenkins}/job/${jobName}/build?token=${jobToken}&cause=Started+by+${upstreamName}"

        //Mask the Auth and Crumb from jenkins
	def response
        wrap([$class: 'MaskPasswordsBuildWrapper', varPasswordPairs: [[password: "${auth}", var: 'AUTH']]]) {
            //Get the crumb from jenkins
            def crumbURL = "$jenkins/crumbIssuer/api/json"
            def crumbResponse = httpRequest url: "$crumbURL", authentication: "${auth}", ignoreSslErrors: true
            def crumb = crumbResponse.content.tokenize(",")
            def crumbName = crumb[2].tokenize('"')[2]
            def crumbValue = crumb[1].tokenize('"')[2]

            //Send out the request
	    wrap([$class: 'MaskPasswordsBuildWrapper', varPasswordPairs: [[password: "${crumbValue}", var: 'CRUMB']]]) {
                response = httpRequest httpMode: 'POST', url:"$remoteURL", authentication: "${auth}", customHeaders: [[name: "$crumbName", value: "$crumbValue"]], ignoreSslErrors: true
	    }
        }

        //Receive the response
        echo "Received response " + response.status
        if (response.status != 201){
            error "Check remote Jenkins instance connectivity and availability of parameter build/build trigger button on job"
        }else{
            echo "$jobName kicked off successfully."
        }
    } catch (e){
	    notifyFailed(recipient, jenkins, jobName)
        error "${e}"
    }
}

def notifyFailed(recipient, jenkins, jobName){
    def sender = "sf-releng@cisco.com"
    def subject = "REMOTE BUILD OF ${jobName} FAILED"
    def details = """<STYLE>BODY,P {  font-family:Verdana,Helvetica,sans serif;  font-size:11px;  color:black;}h1 { color:black; }h2 { color:black; }h3 { color:black; }</STYLE>
                 <BODY>
                 <B style="font-size: 250%;"> <IMG SRC="${JENKINS_URL}/static/e59dfe28/images/32x32/red.gif"/> REMOTE TRIGGER FAILED</B>
                 <p style="font-size: 120%;">Message: Failed to start remote job ${jobName} on ${jenkins}</p>
                 <p style="font-size: 120%;">Build URL: ${env.BUILD_URL}</p>
                 <p style="font-size: 120%;">Console Output: &quot;<a href='${env.BUILD_URL}/console/'> ${env.BUILD_URL}/console/</a>&quot;</p>
				 </BODY>
                 """
	
    emailext subject: subject,
                body: details,
            mimeType: 'text/html',
                  to: recipient,
             replyTo: recipient,
                from: sender
}
