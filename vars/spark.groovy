#!/usr/bin/env groovy

//--------------------------------------------------------------------------
//THIS SHARED LIBRARY WILL READ JSON VALUES FROM A JENKISNJOB
//AND POST RESULTS TO A SPARK ROOM IF EXIST ELSE CREATES NEW ROOM
//TO POST THE RESULTS. COMMITTERS WILL GET ADDED TO THE ROOM AUTOMATICALLY
//TO THESE ROOMS AUTHOR: kpaidi, lfenu
//--------------------------------------------------------------------------

import groovy.json.*

def call(Map config) {
    try {
        withCredentials([string(credentialsId: "${constants.SPARK_CREDENTIAL_ID}", variable: "SPARK_CREDS")]){
            // Get metadata regarding build run via REST API
            log.trace "Getting build metadata from REST API: ${env.BUILD_URL}/api/json?depth=0"
            def buildData = getBuildData("${env.BUILD_URL}/api/json?depth=0")

            // -----------------------------------
            // Figure out Spark room title
            // -----------------------------------
            def roomName = []
            if (config.roomName) {
                roomName << "${config.roomName}"
            } else {
                roomName << "${config.product}"

                if (config.branch) {
                    roomName << "${config.branch}"
                } else if (params.branchName) {
		            roomName << "${params.branchName}"
		        }

                if (config.swarmReview) {
                    //sets room name to a custom swarm review ID
                    roomName << "Review ${config.swarmReview}"
                } else if (config.version) {
                    //sets room name to build version
                    roomName << "Version ${config.version}"
                } else if (params.review) {
		            //sets room name to swarm review from build parameters
                    roomName << "Review ${params.review}"
		        }
            }
            roomName = roomName.join(' > ')

            // -----------------------------------------------------------------
            // Obtain an existing room entity or create one via the Spark API
            // -----------------------------------------------------------------
            def rooms = getRequest("rooms", SPARK_CREDS)
            log.debug "Searching for existing room \'${roomName}\'..."

            //check if the room already existing or not
            log.debug "Searching for existing room \'${roomName}\'..."
            def room = rooms.items.find { it.title == "${roomName}" }

            if (room) {
                log.debug "Found room '${roomName}' with ID: ${room.id}"
                if (config.reviewStatus && config.reviewStatus == "submitted" && config.removeRoom) {
                    deleteRequest(room.id, SPARK_CREDS)
                    return
                } else if (params.status && params.status == "submitted" && config.removeRoom) {
                    //delete's the review room if status is submitted
                    deleteRequest(room.id, SPARK_CREDS)
                    return
                }
            } else {
                // Create room with appropriate title
                log.debug "Did not find existing room '${roomName}'; creating..."
                def payload = ['title': "${roomName}"]

                // If team ID is specified, set them as room owner
                if (config?.teamId) {
                    payload['teamId'] = config.teamId
                }

                //create spark room with the branch details from pipeline
                room = postRequest("rooms", payload, SPARK_CREDS)
            }

            // -----------------------------------------------------------------
            // Figure out which users and message are relevant to the Spark space
            // -----------------------------------------------------------------

            def members = [] // List of members of Spark room
            def owners = [] // List of owners of Spark room
            def message = "## Message sent by: <a href=\"${env.BUILD_URL}\">Jenkins</a>\n\n" // Message to send to Spark room

            if (config.message) {
                message += "### ${config.message}\n\n"
            }

            if (config.swarmReview && config.swarmAuthor) {
                String author = "${config.swarmAuthor}"
                String authorEmail = "${config.swarmAuthor}@cisco.com"
                String authorMarkdown = "<@personEmail:${authorEmail}|${author}>"

                log.debug "Adding relevant committer '${author}' to room..."
                owners << authorEmail

                message += "## Review <a href=\"${constants.SWARM_URL}/changes/${config.swarmReview}\">${config.swarmReview}</a> by ${authorMarkdown}\n\n"
            } else if(params.review){
                String author = getAuthorFromSwarm(params.review)
                String authorEmail = "${author}@cisco.com"
                String authorMarkdown = "<@personEmail:${authorEmail}|${author}>"

                log.debug "Adding relevant committer '${author}' to room..."
                owners << authorEmail
                message += "## Review <a href=\"${constants.SWARM_URL}/changes/${params.review}\">${params.review}</a> by ${authorMarkdown}\n\n"
            } else {
                if(buildData.changeSets) {
                    buildData.changeSets.each { changeSet ->
                        changeSet.items.each {
                            def username = it.author.absoluteUrl.tokenize('/').last()
                            log.debug "Adding relevant committer '${username}' to room..."
                            members << "${username}@cisco.com"
                        }
                    }
                }
            }

            // Make each element of the `owners` list a moderator for the space
            owners.each { k ->
                def payload = ['roomId': room.id,
                           'personEmail': k,
                           'isModerator': true]

                postRequest("memberships", payload, SPARK_CREDS)
            }

            // Make each element of the `members` list a normal user for the space
            members.each { k ->
                def payload = ['roomId': room.id,
                           'personEmail': k,
                           'isModerator': false]

                postRequest("memberships", payload, SPARK_CREDS)
            }

            // Create navigation breadcrumb for original of Spark notification
            def breadcrumb = []

            if (config.branch) {
                    breadcrumb << config.branch
            } else if (params.branchName) {
                breadcrumb << params.branchName
            }

            if (config.version) {
                    breadcrumb << config.version
            }

            if (config.target) {
                    breadcrumb << config.target
            }

            if (! config.result) {
                def buildAborted = commons.getBuildCauses()[1]
                if (buildAborted) {
                    config.result = buildAborted.toString()
                } else {
                    config.result = currentBuild.result ?: "SUCCESS"
                }
            }

            breadcrumb << config.result

            breadcrumb = breadcrumb.findAll().join(' > ')

            message += "### ${breadcrumb}"

            if (config.changelist) {
                message += addChangelist(buildData)
            }

            // If there's a documentation link, place it in the message
            if(config?.docs) {
                message += "\n\n_Troubleshooting Guide: <a href=\"${config.docs}\">${config.docs}</a>_"
            }

            //post the message to spark room
            log.info "Posting message to the Spark room ('${room.title}')"
            log.trace "[SPARK]: calling postRequest: json:\n[roomId: ${room.id}, markdown: ${message}] "
            postRequest("messages", [roomId: room.id, markdown: message], SPARK_CREDS)
	}
    } catch (err) {
        log.err "Could not process spark messages with technical issues on server side.\n${err.getMessage()}"
        throw err
    }
}

//function to read the json objects of the current job
def getBuildData(url) {
    sh "wget -O spark.json --no-check-certificate ${url}"

    def build = readJSON file: "spark.json"

    return build
}


//Get function for reading from an URL
def getRequest(entity, credentials) {

    log.debug "Calling ${constants.WEBEX_API_URL}/${entity}/ using proxy: ${constants.HTTP_PROXY}:${constants.PROXY_PORT}"
    def proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(constants.HTTP_PROXY, constants.PROXY_PORT))
    def jsonSlurper = new JsonSlurperClassic()
    def connection = new URL("${constants.WEBEX_API_URL}/${entity}/").openConnection(proxy);
    def getRC = ""
    connection.setRequestProperty('Accept', 'application/json')
    connection.setRequestProperty('Accept-Charset', 'utf-8')
    connection.setRequestProperty("Content-Type", "application/json")
    connection.setRequestProperty('Authorization', "Bearer ${credentials}" )
    try{
        getRC = connection.getResponseCode();
	    log.trace "[SPARK]:getRequest: Return Code: ${getRC}"
    } catch (err) {
        log.err "[SPARK]:getRequest: Got ${err.getMessage()}."
    }
    def getjsonData = jsonSlurper.parseText(connection.getInputStream().getText());

    return getjsonData
}


//Post function
def postRequest(ID, json, credentials) {
    def proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(constants.HTTP_PROXY, constants.PROXY_PORT))
    def jsonSlurper = new JsonSlurperClassic()
    def type = "application/json; charset=utf-8"
    def rawData = groovy.json.JsonOutput.toJson(json)
    def u = new URL("${constants.WEBEX_API_URL}/${ID}")
    def conn = (HttpURLConnection) u.openConnection(proxy);
    //echo "Sending message to ${constants.WEBEX_API_URL}/${ID}"
    //echo "Posting message: ${json}"

    conn.setDoOutput(true);
    conn.setRequestMethod("POST");
    conn.setRequestProperty( "Content-Type", type );
    conn.setRequestProperty("Authorization", "Bearer ${credentials}")
    conn.setRequestProperty( "Content-Length", String.valueOf(rawData.length()));

    try {
        def os = conn.getOutputStream();
        os.write(rawData.getBytes('UTF-8'))
        os.flush()
        os.close()
        def is = conn.getInputStream();

        log.trace "Response Code: ${conn.getResponseCode()}"

        //if the id is rooms it returns the foom ID's or else returns normal responseCode
        if("${ID}" == "rooms") {
            return jsonSlurper.parseText(is.getText())
        } else {
            return [success: true, responseCode: conn.getResponseCode() ]
        }

    } catch (all) {
        log.trace "Response Code: ${conn.getResponseCode()}"
        return [success: false, exception: conn.getResponseCode() ]
    }

}


//http delete request
def deleteRequest(roomID, credentials) {
    def proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(constants.HTTP_PROXY, constants.PROXY_PORT))
    def jsonSlurper = new JsonSlurperClassic()
    def type = "application/json; charset=utf-8"
    def u = new URL("${constants.WEBEX_API_URL}/rooms/${roomID}")
    def conn = (HttpURLConnection) u.openConnection(proxy);
    conn.setDoOutput(true);
    conn.setRequestMethod("DELETE");
    conn.setRequestProperty( "Content-Type", type );
    conn.setRequestProperty("Authorization", "Bearer ${credentials}")

    println "delete request responseCode is " + conn.getResponseCode()
}

//add changelist to the message
def addChangelist(buildData){
    message = ""
    // Find out what changed in the build and create a changelog
    if(buildData.changeSets) {
	message += "\n\n- - -\n\n"
        buildData.changeSets.each { changeSet ->
            changeSet.items.each { entry ->
                def authorUsername = entry.author.absoluteUrl.tokenize('/').last()
                def authorMarkdown = "<@personEmail:${authorUsername}@cisco.com|${authorUsername}>"

                if(entry._class.endsWith(".P4ChangeEntry")) {
                    // Show changelog from Perforce commits
                    message += "#### Change <a href=\"${constants.SWARM_URL}changes/${entry.changeNumber}\">${entry.changeNumber}</a> (by ${authorMarkdown}): "
                    message += "\n\n```\n${entry.msg}\n```\n"
                } else if(entry._class.endsWith(".GitChangeSet")) {
                    // Show changelog from git commits
                    message += "#### Commit ${entry.id} (by ${authorMarkdown}): "
                    message += "\n\n```\n${entry.msg}\n```\n"
                }

                message += "##### Affected files\n"

                entry.affectedPaths.each { path ->
                    message += " - `${path}`\n"
                }
                message += "\n"
            }
        }
	message += "\n\n- - -\n\n"
    }
    else {
        message += "\n\n Could not get changelist from build data!\n\n"
    }
    return message
}

//function to extract swarm review author using API
def getAuthorFromSwarm(review){

    swarmHost = "${constants.SWARM_URL}${constants.SWARM_API}/reviews/${review}"
    try{
      rep = httpRequest httpMode: 'GET', url: "${swarmHost}", authentication: "swarmProdAPI", ignoreSslErrors: true
      def json = readJSON text: rep.content
      return json.review.author
   }
   catch(e){
      log.err("NON-CRITICAL FAILURE: Cannot get author for ${review} from Swarm")
   }

}
