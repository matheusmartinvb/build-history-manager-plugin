def gitCommit
pipeline {
	agent any

	options {
		disableConcurrentBuilds()
		buildDiscarder(BuildHistoryManager([
			[
				// Keep builds marked to be kept forever
				conditions: [KeptForever()],
				continueAfterMatch: false
			],
			[
				// Delete aborted builds due to no changes
				conditions: [
					BuildResult(matchAborted: true),
					TokenMacro(template: '${BUILD_DISPLAY_NAME}', value: 'Empty'),
					TokenMacro(template: '${onlyIfChanges}', value: 'true')
				],
				actions: [DeleteBuild()],
				continueAfterMatch: false
			],
			[
				// Keep last 30 builds (regardless of status)
				matchAtMost: 30,
				continueAfterMatch: false
			],
			[
				// Keep last 30 successful builds
				conditions: [
					BuildResult(matchSuccess: true)
				],
				matchAtMost: 30,
				continueAfterMatch: false
			],
			[
				// Delete artifacts and logs for everything else
				actions: [
					DeleteArtifacts(),
					[$class: 'DeleteLogFileAction']
				]
			]
		]))
	}

	parameters {
		booleanParam(name: 'onlyIfChanges',
		             description: 'Run the build only if there have been SCM or dependency changes', defaultValue: true)
	}

	triggers {
// 		pollSCM 'H/15 * * * *'
// 		cron('H/30 * * * *')
		cron('H/5 * * * *')
	}

	stages {
		stage('Checkout') {
			steps {
				script {
					gitCommit = checkout(scm).GIT_COMMIT
				}
			}
		}
		stage('Resolve version ranges') {
			steps {
				withMaven(maven: "Maven 3") {
					sh "mvn versions:resolve-ranges -U"
// 					sh "mvn tagging:list-dependencies -DoutputFile=dependencies.txt"
					archiveArtifacts artifacts: 'dependencies.txt'
				}
			}
		}
// 		stage('Check for changes') {
// 			steps {
// 				echo "Previous commit: ${env.GIT_PREVIOUS_COMMIT}"
// 				echo "Current commit: ${gitCommit}"
// 				script {
// 					if (params.onlyIfChanges && gitCommit == env.GIT_PREVIOUS_COMMIT) {
// 						copyArtifacts filter: 'dependencies.txt', fingerprintArtifacts: false, projectName: env.JOB_NAME,
// 						              selector: lastWithArtifacts(), target: 'upstream', optional: true
// 						def oldDependencies =
// 							(fileExists('upstream/dependencies.txt') ? readFile('upstream/dependencies.txt') : '')
// 						def newDependencies = readFile 'dependencies.txt'
// 						if (oldDependencies == newDependencies) {
// 							buildName 'Empty'
// 							currentBuild.result = 'ABORTED'
//                      error('There have been no changes since the last build')
// 						}
// 					}
// 				}
// 			}
// 		}
// 		stage('Set build number') {
// 			steps {
// 				script {
// 					def buildNumber;
// 					lock ('tagging-betwave-server') {
// 						buildNumber = tagBuildNumber scm
// 					}
//
// 					withMaven(maven: "Maven 3") {
// 						sh "mvn tagging:build-number -DbuildNumber=${buildNumber}"
// 					}
//
// 					def pom = readMavenPom file: "betwave/betwave-root/pom.xml"
// 					buildName pom.version
//
// 					writeFile file: 'version.properties', text: "version=${pom.version}"
// 					archiveArtifacts artifacts: 'version.properties'
// 				}
// 			}
// 		}
		stage('Build & Deploy') {
			steps {
				withMaven(maven: "Maven 3", options: [artifactsPublisher(disabled: true)]) {
// 					sh "mvn clean deploy jib:build"
					sh "mvn clean install -DskipTests"
				}
			}
		}
	}

// 	post {
// 		failure {
// 			emailext subject: '$DEFAULT_SUBJECT',
// 			         body: '$DEFAULT_CONTENT',
// 			         recipientProviders: [
// 				         [$class: 'CulpritsRecipientProvider'],
// 				         [$class: 'DevelopersRecipientProvider'],
// 				         [$class: 'RequesterRecipientProvider']
// 			         ],
// 			         replyTo: '$DEFAULT_REPLYTO',
// 			         to: '$DEFAULT_RECIPIENTS'
// 		}
// 	}
}
