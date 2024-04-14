timeout(60) {
    node("maven-slave") {
        wrap([class: 'BuildUser']) {
            currentBuild.description = """
User: ${BUILD_USER}
Brunch: $BRANCH
"""
            config = readYaml text: env.YAML_CONFIG

            if (config != null) {
                for(param in config.entrySet()) {
                    env.setProperty(param.getKey(), param.getValue())
                }
            }
            testTypes = env.getProperty('TEST_TYPES').replace("[", "").replace("]", "").split(",\\s*")

        }

        def jobs = [:]

        def triggeredJobs = [:]

        for(type in testTypes) {
            jobs[type] = {
                node('maven-slave') {
                    stage("Running $type testts"){
                        triggeredJobs[type] = build(job: "$type-tests", parameters: [
                            text(name: 'YAML_CONFIG', value: env.YAML_CONFIG)
                    ])
                    }
                }
            }
        }

        parallel jobs

        stage("Create additional allure report artifacts") {
            dir ("allure-results") {
                sh "echo BROWSER=${env.getProperty('BROWSER')} > enviroments.txt"
                sh "echo TEST_VERSION=${env.getProperty('TEST_VERSION')} >> enviroments.txt"
            }
        }

        stage("Copy allure reports") {
            dir ("allure-results"){
                for(type in testTypes){
                    copyArtifacts filter: "allure-report.zip", projectName: "${triggeredJobs[type].projectName}", selector: lastSoccesful(), optional: true
                    sh "unzip ./allure-report.zip -d ."
                    sh "rm -rf ./allure-report.zip"
                }
            }
        }
        stage("Publish allure reports"){
            dir ("allure-results"){
                allure([
                        results: ["."],
                        reportBuildPolicy: ALWAYS
                ])
            }
        }
    }
}