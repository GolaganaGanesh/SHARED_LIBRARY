#!/usr/bin/env groovy

//--------------------------------------------------------------------------
//THIS SHARED LIBRARY WILL HOLD GLOBAL VARIABLES THAT CAN BE ACCESSED FROM
//OTHER JENKINSFILES OR LIBRARY METHODS
//AUTHOR: lfenu
//--------------------------------------------------------------------------

import groovy.json.*

class constants {
    // Perforce
    final String PERFORCE_SERVER = 'sbg-perforce.esl.cisco.com:1666'
    final String PERFORCE_USER = 'builders'
    final String P4TICKET_VALUE = '17544F7BAC4D9D87B690C6C77970B116'
    final String PERFORCE_CREDENTIALS = '0020ddcc-093f-4bb1-90f5-42773cf86ea3'

    // other credentials
    final String SPARK_CREDENTIAL_ID = "e0ba6a4d-8c73-4ff9-8344-8e611888ecba"
    final String BUILDERS_P4_CRED = "swarmProdAPI"
    final String BUILDERS_P4_AUTH = "Basic YnVpbGRlcnM6MWZjODA2ZDMyYTBkZDgzMzk5MzlkMjRlYmZjOTRmZDk="
    final String BUILDERS_JENKINS_API_ID = "jenkinsAPIToken"

    // URLs
    final String WEBEX_API_URL = "https://webexapis.com/v1"
    final String SWARM_URL = "https://sp4-fp-swarm.cisco.com"
    final String SWARM_API = "/api/v9"

    // Decoration strings
    final String INFRA_TAG = '[<a style=color:red;>INFRA</a>]'

    // PROXY
    final String HTTP_PROXY = "proxy.esl.cisco.com"
    final int PROXY_PORT = 80
}
