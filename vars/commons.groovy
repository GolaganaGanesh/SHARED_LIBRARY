#!/usr/bin/env groovy

//--------------------------------------------------------------------------
// httpRequest shared library
//--------------------------------------------------------------------------

import groovy.json.*
import groovy.time.*
import org.codehaus.groovy.runtime.StackTraceUtils

def httpRequest(config = [:]) {
    def logPrefix = "[commons.httpRequest]: "
    config.creds = config.creds ?: constants.BUILDERS_P4_CRED
    withCredentials([usernameColonPassword(credentialsId: config.creds, variable: 'CREDS')]) {
        try {
            def response

            log.trace "${config.creds}"
            // set defaults
            config.url = config.url ?: constants.SWARM_URL + "/queue/status"
            config.method = config.method ?: "GET"
            config.jstr = config.jstr ?: "null"


            HttpURLConnection http = new URL(config.url).openConnection()
            // set connection method, default GET
            http.setRequestMethod(config.method)
            // set connection output to true
            http.setDoOutput(true)

            http.setRequestProperty("Content-Type", "application/json")
            http.setRequestProperty("Authorization", "Basic " + CREDS.bytes.encodeBase64().toString())

            // if json provided open an outputStream

            if (config.method != "GET" && config.jstr) {
                http.outputStream.write(config.jstr.getBytes("UTF-8"))
            }

            log.trace "${logPrefix} http config: ${config}"

            // set additional properties if provided
            config.prop.each { k, v ->
                log.debug("${logPrefix} Setting properties:  \"${k}\":  \"${v}\"" )
                http.setRequestProperty("${k}", "${v}")
            }

            http.connect()

            log.trace "${logPrefix} http.responseCode: ${http.responseCode}"

            // check if response code is OK
            // lets cover HttpURLConnection.HTTP_OK/CREATED/ACCEPTED

            if (http.responseCode in [ 200,201,202 ]) {
                def httpInputStreamText = http.inputStream.getText('UTF-8')
                log.trace "${logPrefix} httpInputStreamText: ${httpInputStreamText.getClass()}"

                // ignore response if not json
                // need to come with better solution
                try {
                    response = new JsonSlurperClassic().parseText(httpInputStreamText)
                } catch(ignored) {
                    log.comment "${logPrefix} can not parse text, seems to be not a json format"
                }
            } else {
                try {
                    response = new JsonSlurperClassic().parseText(http.errorStream.getText('UTF-8'))
                } catch(ignored) {
                    response = http.errorStream.getText('UTF-8')
                }
                log.err "${Thread.currentThread().stackTrace[10].methodName}: ${response}"
            }

            log.trace "${logPrefix} httpResponse: ${response.getClass()}\n${response}"
            http.disconnect()
            return response
        } catch (MalformedURLException e) {
            log.warn "${logPrefix} MalformedURLException: ${Thread.currentThread().stackTrace[10].methodName}: ${e.toString()}"
        } catch (IOException e) {
            log.err  "${logPrefix} IOException: ${Thread.currentThread().stackTrace[10].methodName}: ${e.toString()}"
        }
   }
}

// Check how many space on the node host
// if not enough for the build, take the node offline
// and rebuild
def checkDiscUsage(config = [:]) {
    def gTok = 1048576
    def defaults = [ min: 0, partition: "/data", rebuild: true, nodeOffline: true ]
    defaults.each { k, v ->
        if (!config[k]) { config[k] = v }
    }

    log.trace "[commons.checkDiscUsage]: CONFIG: ${config}"
    log.debug (sh (script:"df -h ${config.partition}", returnStdout: true))
    def currntAmount = sh (script: """ df ${config.partition} | \
                            awk -v p=${config.partition} '\$0 ~ p {print \$(NF-2)}' """,
                            returnStdout: true)

    currntAmount = currntAmount.replaceAll('G','').toFloat() / gTok
    log.info "DU on ${NODE_NAME}:${config.partition}: ${currntAmount.round(2)}, required: ${config.min}"
    if (currntAmount < config.min ) {
        def message = "Not enough space on ${env.NODE_NAME}:${config.partition}." +
                      "\nExpected: ${config.min}, has: ${currntAmount.round(2)}"
        if (config.nodeOffline) {
            log.trace "nodeOffline: ${config.nodeOffline}"
            markNodeOffline(env.NODE_NAME,"Due to space issue.")
        }
        if (config.rebuild) {
            log.warn "rebuild: ${config.rebuild}"
            reBuild()
        }
        log.failBuild message
    }
}

def getBuildCauses(build_url = BUILD_URL) {
    def url = "${build_url}api/json?tree=actions[causes[*]]"
    def response = httpRequest(["url":"${url}", prop: ["Authorization": '']])
    log.trace "RESPONSE: ${response.getClass()}: ${response}"
    return  response.actions.causes.shortDescription
}


// todo: add spark notification
def markNodeOffline(node_name, message) {
    computer = Jenkins.instance.getNode(node_name).toComputer()
    if ( computer.isOnline()) {
        log.critical "Taking the ${node_name} offline\n ${log.Symbols.point} ${message}"
        computer.setTemporarilyOffline(true)
        computer.doChangeOfflineCause(message)
        computer = null
    } else {
        log.warn "The ${node_name} is already offline"
    }
}

def addSummary(message, icon="") {
    // if node name, create a link
    if (message == NODE_NAME) {
        message = "on <a href=${JENKINS_URL}/computer/${NODE_NAME}>${NODE_NAME}</a>"
        icon = icon ?: "terminal.png"
    } else if (message.split(" ")[-1] == "seconds") {
        icon = icon ?: "16x16/clock.png"
    }
    log.trace "[commons.addSummary] icon: ${icon}"
    icon = icon ?: "notepad.png"
    manager.createSummary(icon).appendText(message.toString())
}

def reBuild(){
    def currentBuildparameters = currentBuild.getRawBuild()?.actions.find{ it instanceof ParametersAction }?.parameters
    def message =  currentBuildparameters.each {
        println it.dump()
    }
    log.trace "[commons.reBuild]: currentBuild parameters\n${message}"
    build(wait: false, job: JOB_NAME, parameters: currentBuildparameters)
}

def MlsToHumanTime(start, end=0) {
    if (!end) { end = start; start = 0 }
    elapsedTime = end.toInteger() - start.toInteger()
    int second = (elapsedTime / 1000).longValue() % 60;
    int minute = (elapsedTime / (1000 * 60)).longValue() % 60;
    int hour = (elapsedTime / (1000 * 60 * 60)).longValue() % 24;
    int millis = elapsedTime % 1000
    def total = new TimeDuration(hour, minute, second, millis)
    log.trace "[commons.toHumanTime] total: ${total}"
    return total.toString()
}

def StopJenkinsJob(url) {
    log.trace "StopJenkinsJob:URL: ${url}"
    log.trace "${constants.BUILDERS_JENKINS_API_ID}"
    def response = httpRequest([url: "${url}api/json", creds: constants.BUILDERS_JENKINS_API_ID])
    log.debug "StopJenkinsJob:Current buildURL is ${env.BUILD_URL}"
    log.debug "StopJenkinsJob:Provided URL is ${url}"

    if (response.building) {
        log.warn "Trying to stop ${url} becuase new build started"
        httpRequest([url: "${url}stop", method: "POST", creds: constants.BUILDERS_JENKINS_API_ID])
    }
}

@NonCPS
def arrayToJsonString(arr = [:]) {
   def json = new JsonBuilder()
   def root = json arr
   return  JsonOutput.prettyPrint(json.toString())
}

