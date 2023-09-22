import groovy.json.JsonSlurper

// -----------------------------------------------------------------------
// This function returns the parameters set from the last successful build
// from an upstream IMS job
// ----------------------------------------------------------------------
def call(String upstreamTrigger = "WRAPPER", String jenkinsURL = "https://firepower-test.service.ntd.ciscolabs.com") {

    // Whitelist which jobs we will look for parameters in
    def ALLOWED = ['WRAPPER', 'DISTRIBUTABLES']
    if (!ALLOWED.contains(upstreamTrigger)) {
        error "Invalid upstream trigger selected!"
    }

    // Assume that the downstream job is in a branch folder within the IMS folder
    def parentPath = env.JOB_NAME.tokenize('/')[0..1]

    // We've identified the absolute path to the upstream job
    def upstreamJobPath = parentPath + [upstreamTrigger]

    // Reformat the job path so it's URL friendly
    def upstreamURLPath = upstreamJobPath.collect { "job/$it" }.join('/')

    // Make an HTTP request to the Jenkins instance and get results via the JSON API
    def rx = httpRequest "${jenkinsURL}/${upstreamURLPath}/lastSuccessfulBuild/api/json"
    def rxJson = new JsonSlurper().parseText(rx.getContent())

    // Find the `parameters` payload in the HTTP response
    def parameters = rxJson['actions'].find { it.containsKey("parameters") }['parameters']

    // Return a map of the parameter name and value pair
    return parameters.collectEntries {
        [(it.name): it.value]
    }
}