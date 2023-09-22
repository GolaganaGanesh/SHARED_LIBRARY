#!/usr/bin/env groovy

//--------------------------------------------------------------------------
// This shared library will colorized message
//--------------------------------------------------------------------------

import groovy.json.*
import org.codehaus.groovy.runtime.StackTraceUtils
import groovy.transform.Field

// import jenkins.model.*
@Field  PERFORCE_CREDENTIALS = env.PERFORCE_CREDENTIALS ?: constants.PERFORCE_CREDENTIALS
@Field  PERFORCE_SERVER = env.PERFORCE_SERVER ?: constants.PERFORCE_SERVER
@Field  SPEC_PARAMS = ["allwrite", "backup", "changeView", "clobber", "compress", "line", "locked", "modtime", "rmdir", "serverID", "streamName", "type"]


//Get the workspace for the checkout
def getP4Workspace(config = [:]) {
    def reviewSuffix = ''
    def spec = getClientSpecVal(config)
    log.trace "SPEC: ${spec}"

    // if no view config or depot path provided - fail
    if ( ! config.depotPath && ! config.view ) {
        log.failBuild ("Please provide either view spec or comma separated depots paths array")
    }

    // define default client name, add review number if exist
    if (params.review) { reviewSuffix = "-${params.review}" }
    config.name = config.name ?: "${D_P4USER}:${JOB_NAME.replaceAll("/","-")}-${BUILD_NUMBER}${reviewSuffix}"

    // if view lines provided add to spec
    // otherwise let generate from depotPath
    // dir name optional
    if (config.view) {
        spec.view = config.view
    } else {
        spec.view = getViewSpec(config.depotPath, config.name, config.dir)
    }

    log.trace "[perforce.getP4Workspace]: SPEC:\n${spec}"

	def ws = manualSpec(
            name: config.name,
            // charset: config.charset ?: 'none',
            // pinHost: config.pinHost ?: false,
            spec: clientSpec(spec)
        )
    log.trace "[perforce.getP4Workspace]: WS: ${ws}"
    Checkout(ws)
	return ws
}

// Code checkout function
def Checkout(workspace) {
	checkout(changelog: false,
		poll: false,
		scm: [$class: 'PerforceScm',
			credential: PERFORCE_CREDENTIALS,
			populate: [$class: 'AutoCleanImpl',
				delete: true,
				modtime: false,
				parallel: [enable: true,
					minbytes: '1024',
					minfiles: '1',
					threads: '6'],
				quiet: true,
				replace: true],
		workspace: workspace])
}

def getWS(){
     // Define workspace
    def ws = [$class: 'ManualWorkspaceImpl',
    charset: 'none', name: 'builders:jenkins-${JOB_NAME}',
    pinHost: false, spec: clientSpec(view: '//depot/admin/... //builders:jenkins-${JOB_NAME}/...')]


    // Create object
    def p4 = p4(credential: PERFORCE_CREDENTIALS, workspace: ws)
    log.info p4.run('client', '-o')
    log.info 'done'
}

def getClientSpecVal(config = [:]) {
    def spec = [:]
    // all clients we create for pre-commit job
    // has a default parameters, expect of the clobber:true
    // but in case we still want to overwrite some of defalts
    // want to read all config to define the spec map
    config.each { k,v ->
        if ( k in SPEC_PARAMS ) {
            spec."${k}" = v
        }
    }

    // set clober to true if not define
    spec.clobber = spec.clobber ?: true
    log.trace "[getClientSpecVal]: SPEC: ${spec}"
    return spec
}

def getViewSpec(depotPath, clientName, dirName = "" ) {
    def view = ''
    def target = "//${clientName}"
    if (dirName) { target = "${target}/${dirName}" }
    depotPath.split(',').collect {
        view += "${it}/... ${target}/...\n"
    }
    log.trace "[getViewSpec]: VIEW: ${view}"
    return view
}